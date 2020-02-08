/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.exception

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.ToString
import org.simplemes.eframe.domain.validate.ValidationError

/**
 * This class encapsulates multiple response messages (error, etc) and allows controllers to render them
 * in a consistent format.  This holder will use the message with highest severity for the top-level
 * message.  Any other messages will be listed in the 'moreMessages' section (if needed).
 *
 * <p/>
 * A typical example for responding with multiple messages within a controller is shown below:<p/>
 * <pre>
 *
 * def msgHolder = new MessageHolder(text: "Bad Argument='order'", code: 101)
 * msgHolder.addInfo(text: 'Sample = SN4629697')
 * msgHolder.addWarn(text: 'Adjustment = 102.5', code: 1043)
 * msgHolder.addError(text: 'Adjustment Failed on S/N = SN4629697', code: 2039)
 * msgHolder.render(this)
 *  // or
 * def holder = new MessageHolder(level: MessageHolder.LEVEL_INFO, text: 'Saved Records')
 * </pre>
 *
 * <b>Note:</b> The order of the messages is not guaranteed.
 */
@ToString(includeNames = true, includePackage = false)
@JsonSerialize(using = MessageHolderSerializer)
class MessageHolder {

  /**
   * The severity level (int) for error messages (highest severity).
   * The text form of these levels are stored in levelText.
   */
  public static final int LEVEL_ERROR = 0

  /**
   * The severity level (int) for warning messages (middle severity).
   * The text form of these levels are stored in levelText.
   */
  public static final int LEVEL_WARN = 1

  /**
   * The severity level (int) for information messages (lowest severity).
   * The text form of these levels are stored in levelText.
   */
  public static final int LEVEL_INFO = 2

  /**
   * The severity level string for error messages (highest severity).
   */
  public static final String LEVEL_ERROR_STRING = 'error'

  /**
   * The severity level string for warning messages (middle severity).
   */
  public static final String LEVEL_WARN_STRING = 'warning'

  /**
   * The severity level string for information messages (lowest severity).
   */
  public static final String LEVEL_INFO_STRING = 'info'

  static final List<String> levelText = [LEVEL_ERROR_STRING, LEVEL_WARN_STRING, LEVEL_INFO_STRING]

  /**
   * The request context attribute used to store the message holder that accumulates messages for
   * a controller to display.  This allows deeply-nested methods to return a message to the user without
   * affecting all intermediate method calls.
   */
  //public static final String MESSAGE_HOLDER_REQUEST_ATTRIBUTE = '_MessageHolder'

  /**
   * The overall severity level of the message(s).
   */
  int level = LEVEL_ERROR

  /**
   * The text of the primary the message.
   */
  String text

  /**
   * The message code of the primary the message.
   */
  int code = 0

  /**
   * A list of other messages.
   */
  List<MessageHolder> otherMessages

  /**
   * Optional URi to specify the page that collects additional parameters.
   */
  //String additionalDataURI

  /**
   * The key used to store the MPE data in the request as an attribute.
   */
  //String mpeKey

  /**
   * Empty constructor.
   */
  MessageHolder() {

  }

  /**
   * Standard Map constructor.
   * @param errors The validation errors from a domain object that failed validation.
   */
  MessageHolder(Map options) {
    options.each { k, v -> this[(String) k] = v }
  }
  /**
   * Builds a message holder with the given messages.  All messages are marked as errors.
   * @param errors The validation errors from a domain object that failed validation.
   */
  MessageHolder(List<ValidationError> errors) {
    for (error in errors) {
      addError(code: error.code, text: error.toString())
    }
  }


  /**
   * Adds single error message to the holder.
   *
   * @param map The map with the parameters (text, code and level are supported). (Modified).
   * @return This message holder.
   */
  MessageHolder addError(Map map) {
    map.level = LEVEL_ERROR
    add(map)
    return this
  }

  /**
   * Adds single info message to the holder.
   *
   * @param map The map with the parameters (text, code and level are supported). (Modified).
   * @return This message holder.
   */
  MessageHolder addInfo(Map map) {
    map.level = LEVEL_INFO
    add(map)
    return this
  }

  /**
   * Adds single warning message to the holder.
   *
   * @param map The map with the parameters (text, code and level are supported). (Modified).
   * @return This message holder.
   */
  MessageHolder addWarn(Map map) {
    map.level = LEVEL_WARN
    add(map)
    return this
  }

  /**
   * Adds single warning message to the holder (convenience method).
   *
   * @param map The map with the parameters (text, code and level are supported). (Modified).
   * @return This message holder.
   */
  MessageHolder addWarning(Map map) {
    return addWarn(map)
  }

  /**
   * Internal add method.  Used by all of the convenience methods (e.g. addInfo(), etc).
   * @param map The map with the parameters (text, code and level are supported).
   */
  protected add(Map map) {
    if (!text) {
      // No primary message, so just use this message as the new primary.
      text = map.text
      level = (int) map.level
      if (map.code) {
        code = (int) map.code
      }
      return
    }

    // Add to the list of additional messages.
    if (otherMessages == null) {
      otherMessages = []
    }

    def messageHolder = new MessageHolder(map)
    otherMessages << messageHolder

    // Then swap if the new message's level is higher than the primary.
    if (level > (int) map.level) {
      // Should swap since the new level is more important than the old one.
      swap(messageHolder)
    }
  }

  /**
   * Swaps the given message from the otherMessages list with the main message.
   * @param msgIndex The index of the message to swap.
   */
  protected void swap(MessageHolder msg) {
    def tmpText = text
    def tmpLevel = level
    def tmpCode = code
    text = msg.text
    level = msg.level
    code = msg.code
    msg.text = tmpText
    msg.level = tmpLevel
    msg.code = tmpCode
  }

  /**
   * Returns the level (as text).
   * @return The level.
   */
  String getLevelText() {
    return levelText[level % levelText.size()]
  }

  /**
   * Renders the current messages(s) for the given controller in the planned response format.<p/>
   * <p/>
   * <b>Note:</b> This should be called after the response.status code is set.  Once output is sent to
   * the response, the status code can't be changed (Grails 2.4.x change).
   * @param controller The controller the needs the rendering done for it.
   */
/*
  public void render(controller) {
    if (!text) {
      throw new IllegalArgumentException("text is required");
    }
    forceResponseCodeIfNeeded(controller)

    // Kludge to force JSON output when needed since Accept headers are not working.
    // https://jira.grails.org/browse/GRAILS-10643
    def format = controller.response.format  // Default to XML
    if (controller.params.format == 'json') {
      format = controller.params.format
    }
    //println "format = $format"

    if (format == 'json') {
      def res = [:]
      res.message = [level: level, text: text, code: code]
      if (additionalDataURI) {
        res.message.additionalDataURI = additionalDataURI
      }
      if (mpeKey) {
        res.message.mpeKey = mpeKey
      }
      if (otherMessages) {
        res.message.moreMessages = []
        for (message in otherMessages) {
          res.message.moreMessages << [level: message.level, text: message.text, code: message.code]
        }
      }
      controller.render res as JSON
    } else {
      def writer = new StringWriter()
      def xml = new MarkupBuilder(writer)
      xml.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')
      xml.message {
        //noinspection GroovyAssignabilityCheck
        level(level)
        text("${text}")
        code(code)
        additionalDataURI(additionalDataURI)
        mpeKey(mpeKey)
        for (msg in otherMessages) {
          message {
            level(msg.level)
            text("${msg.text}")
            code(msg.code)
          }
        }
      }
      controller.render(contentType: ControllerUtils.CONTENT_TYPE_XML, text: writer.toString())
    }
  }

*/
  /**
   * Forces the HTTP response code to HTTP_ACCEPTED (202) if needed.
   * If there is an error and the HTTP response code is Ok (200, 204 or 302), then the response code
   * is forced to HTTP_ACCEPTED.
   * @param controller The controller handling this request.
   */
/*
  protected void forceResponseCodeIfNeeded(controller) {
    if (level != LEVEL_ERROR) {
      // Only force change on errors
      return
    }
    if (controller.response.status == ControllerUtils.HTTP_OK ||
            controller.response.status == ControllerUtils.HTTP_OK_NO_CONTENT ||
            controller.response.status == ControllerUtils.HTTP_FOUND) {
      //println "Forced response from ${controller.response.status}"
      controller.response.status = ControllerUtils.HTTP_ACCEPTED
    }
  }
*/

  /**
   * Returns the message holder that can accumulate messages for any controller.  This stored in
   * the request context and is typically used by a controller to render the responses to a dashboard.
   * This allows deeply-nested methods to return a message to the user without
   * affecting all intermediate method calls.
   * <p/>
   * A short of a controller method is shown below:
   * <pre>
   * def createRMA() &#123;
   *   rmaService.createRMA(params.serialNumber)
   *   MessageHolder.requestMessageHolder.renderResponse(this)
   * &#125;
   * </pre>
   * <p/>
   * The <code>rmaService</code> above might eventually call a method that needs to
   * return a non-error message to the user:
   * <pre>
   * def resolveSerialNumber(String serialNumber) &#123;
   *   ...
   *   // Let user know what part number we used.
   *   MessageHolder.requestMessageHolder.addInfo("Created RMA for ${serialNumber} with part ${partNumber}")
   * &#125;
   * </pre>
   * This allows many types of messages (info, error, warning) to be returned to the user.
   * @return The request's message holder (never null).
   */
/*
  public static MessageHolder getRequestMessageHolder() {
    def attrs = RequestContextHolder.requestAttributes
    def msgHolder = attrs.getAttribute(MESSAGE_HOLDER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
    if (!msgHolder) {
      msgHolder = new MessageHolder()
      attrs.setAttribute(MESSAGE_HOLDER_REQUEST_ATTRIBUTE, msgHolder, RequestAttributes.SCOPE_REQUEST)
    }
    return msgHolder

  }
*/

}
