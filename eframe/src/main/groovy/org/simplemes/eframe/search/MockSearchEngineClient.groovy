/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.util.logging.Slf4j
import org.apache.http.HttpHost
import org.simplemes.eframe.misc.TypeUtils

/**
 * This mock client simulates the search interface to the external search engine (Elastic Search in this case).
 * This is used in production when the search engine is not configured.
 */
@Slf4j
class MockSearchEngineClient implements SearchEngineClientInterface {

  /**
   * The list of external search engines that the RestClient will connect to.  Typically, this is defined
   * in the application.yml configuration file.
   */
  List<HttpHost> hosts

  /**
   * The results to be returned from the indexObject() call.  Defaults to a 'created' result.
   */
  Map indexObjectResults

  /**
   * The simulated search results returned by the mock globalSearch() method.
   */
  SearchResult globalSearchResult

  /**
   * The simulated search results from the domainSearch() method.
   */
  SearchResult domainSearchResult

  /**
   * The expected domain class used by the domainSearch method.
   */
  Class expectedDomainClass

  /**
   * The actions performed on this mock client.  This will be list of maps with the elements
   * provided by each mock method.  The mock verification method: verify() is used to
   * check this list for the expected action.
   */
  List<Map> actions = []

  /**
   * Defines a map with the result used for a corresponding object in the input list.  Used to simulate
   * a failure in the bulk index on the server.
   * Example: [null,null,'failed']
   */
  List bulkIndexResult = []

  /**
   * The mocked status for the getStatus() requests.
   */
  SearchStatus searchStatus

  /**
   * The expected query string.
   */
  String expectedQueryString

  /**
   * The expected query parameters for the global Search.
   */
  Map expectedParams

  /**
   * Returns the status of the search engine's cluster.
   * <p/>
   * This mock method will store an action: [action: 'getStatus']
   * @return The status.  This mock returns a status of 'green'.
   */
  SearchStatus getStatus() {
    actions << [action: 'getStatus']
    def res = searchStatus ?: new SearchStatus([status: "green"])
    log.trace('getStatus: Mock used {}', res)
    return res
  }

  /**
   * Indexes a single object, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   *
   * <p/>
   * This mock method will store an action: [action: 'indexObject', object: object]
   *
   * @param object The domain object to index.  Must be saved.
   * @return Status of the index creation.  A Map.
   */
  @SuppressWarnings("PrintStackTrace")
  Map indexObject(Object object) {
    log.trace('indexObject: Mock used.  Object = {}', object)

    actions << [action: 'indexObject', object: object]
    return indexObjectResults ?: [_index: "sample", _id: "${object?.uuid}", result: "created"]
  }

  /**
   * Removes a single object from the index.
   * <p/>
   * This mock method will store an action: [action: 'removeObjectFromIndex', object: object]
   *
   * @param object The domain object to remove from the index.  Must have a record id..
   * @return Status of the index creation.  A Map.
   */
  Map removeObjectFromIndex(Object object) {
    log.trace('indexObject: Mock used.  Object = {}', object)
    actions << [action: 'removeObjectFromIndex', object: object]
    return indexObjectResults ?: [_index: "sample", _id: "$object.uuid", result: "deleted"]
  }

  /**
   * Performs a bulk index on a list of objects, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   *
   * <p/>
   * This mock method will store an action: [action: 'bulkIndex', objects: objects]
   *
   * @param object The domain objects to index.  Must be saved.
   * @param archiveReferenceList The list of archive references associated with the list of objects.  (Optional).
   *        This will modify the index used to be the archive index and add the element '_archiveReference' to the
   *        document indexed.
   * @return Status of the index creation.  A Map.
   */
  Map bulkIndex(List objects, List<String> archiveReferenceList = null) {
    log.trace('bulkIndex: Mock used')
    actions << [action: 'bulkIndex', object: objects]
    def items = []
    def i = 0
    for (object in objects) {
      def indexName = SearchHelper.instance.getIndexNameForDomain(object.getClass())
      // Use the provide result list to determine the result.  Otherwise, defaults to 'created'.
      def result = bulkIndexResult.size() > i ? bulkIndexResult[i] ?: 'created' : 'created'
      items << [index: [_index: indexName, _id: object.uuid, result: result]]
      i++
    }

    return indexObjectResults ?: [took: 30, errors: false, items: items, archiveReferenceList: archiveReferenceList]
  }

  /**
   * Deletes all Indices in the search engine.  This will lose all data there.
   * Can log performance as level INFO log messages.
   *
   * @return Status of the removal.
   */
  @Override
  Map deleteAllIndices() {
    log.trace('deleteAllIndices: Mock used')
    actions << [action: 'deleteAllIndices']
    return [acknowledged: true]
  }

  /**
   * This is a mock utility method that will verify that the given action was performed on this Mock client.
   * @param expectedAction An action that is expected.  Example: [action: 'indexObject', object: object]  If null,
   *        then not actions should be logged to pass this verification.
   */
  void verify(Map expectedAction) {
    if (expectedAction) {
      for (action in actions) {
        if (compareActions(action, expectedAction)) {
          return
        }
      }
      // Make a nice readable form of the two actions that don't match.
      def se = expectedAction?.action
      def sa = actions[0]?.action

      if (expectedAction?.object instanceof List) {
        se += "["
        for (o in expectedAction?.object) {
          se += "${TypeUtils.toShortString(o)},"
        }
        se += "]"
      }
      if (actions[0]?.object instanceof List) {
        sa += "["
        for (o in actions[0]?.object) {
          sa += "${TypeUtils.toShortString(o)},"
        }
        sa += "]"
      }

      assert expectedAction == null, "Expected action $se is not in the actions called in this Mock: $sa.  \nFull Data: $expectedAction \n Actions Called: $actions"
    } else {
      // No action should be called.
      assert actions.size() == 0, "No Mock Client actions should have been called during the test.  Actions called: $actions"
    }
  }

  /**
   * Compares two Maps for matching expected actions.
   * @param a
   * @param b
   * @return True if they are the same.
   */
  boolean compareActions(Map a, Map b) {
    log.error("Comparing A ${a.object.getClass()}, B ${b.object.getClass()}")
    if (a?.equals(b)) {
      // Simple test with exact matching Maps.
      return true
    }
    if (a.action != b.action) {
      log.warn("Mock Client action $a.action does not match $b.action")
      return false
    }
    if (a.object instanceof Collection && b.object instanceof Collection) {
      // Check the list of objects
      if (a.object.size() != b.object.size()) {
        log.warn("Mock Client action size ${a.object.size()} does not match ${b.object.size()}")
        return false
      }
      for (o in a.object) {
        if (!b.object.contains(o)) {
          log.warn("Mock Client actions does not contain expected action ${o}.  Actions = ${b.object}")
          return false
        }
      }
      log.warn("Both objects are lists and all of B is in A. A ${a.object}, B ${b.object}")
      return true
    }

    log.warn("Both objects are not lists or are missing one row. A ${a.object.getClass()}, B ${b.object.getClass()}")

    return a.object?.equals(b.object)
  }

  /**
   * Performs a mock global search using the given query string.
   * @param query The query string.  If it starts with &#123; then the string is used as the body for the GET request.
   * @param params Optional query parameters.  Supported elements: from and size.
   * @param The search results.   Can return the globalSearchResult if provided.
   */
  @Override
  SearchResult globalSearch(String query, Map params = null) {
    if (expectedQueryString) {
      assert query == expectedQueryString
    }
    if (expectedParams) {
      assert params == expectedParams
    }
    if (globalSearchResult) {
      return globalSearchResult
    } else {
      def searchResult = new SearchResult()
      searchResult.query = query
      searchResult.totalHits = 2
      searchResult.elapsedTime = 137
      def hit = new SearchHit()
      hit.className = 'sample.DummyDomain'
      hit.uuid = UUID.randomUUID()
      searchResult.hits << hit
      return searchResult
    }
  }

  /**
   * Performs a standard domain search with the query string.  The SearchHelper will fallback to an internal search in
   * some cases.
   * @param domainClass The domain class to search.
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: from and size
   * @return The search result, containing the list of values found.
   */
  SearchResult domainSearch(Class domainClass, String query, Map params = null) {
    if (!SearchHelper.instance.isSearchable(domainClass)) {
      throw new IllegalArgumentException("Domain ${domainClass} is not searchable.")
    }
    if (expectedQueryString) {
      assert query == expectedQueryString
    }
    if (expectedParams) {
      assert params == expectedParams
    }
    if (domainSearchResult) {
      domainSearchResult.query = query
      return domainSearchResult
    } else {
      def searchResult = new SearchResult()
      searchResult.query = query
      searchResult.totalHits = 2
      searchResult.elapsedTime = 137
      def hit = new SearchHit()
      hit.className = 'sample.DummyDomain'
      hit.uuid = UUID.randomUUID()
      searchResult.hits << hit
      return searchResult
    }
  }

}
