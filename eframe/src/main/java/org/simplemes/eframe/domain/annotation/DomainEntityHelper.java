package org.simplemes.eframe.domain.annotation;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

import groovy.lang.Closure;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.model.naming.NamingStrategy;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.TransactionCallback;
import io.micronaut.transaction.TransactionStatus;
import io.micronaut.transaction.jdbc.DataSourceUtils;
import org.simplemes.eframe.domain.PersistentProperty;
import org.simplemes.eframe.domain.validate.ValidationError;
import org.simplemes.eframe.domain.validate.ValidationErrorInterface;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Support methods for the @DomainEntity annotation.  Provides much of the logic injected into the domain
 * class.  This includes support for methods like list(), findById(), save(), etc.
 */
public class DomainEntityHelper {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  protected static DomainEntityHelper instance = new DomainEntityHelper();

  /**
   * The name of the transient holder for internal domain settings.
   * This Map holds things like lists of records to be deleted on update.
   */
  public static final String DOMAIN_SETTINGS_FIELD_NAME = "_domainSettings";

  /**
   * The name of the element in the domain settings holder that will contain the list of loaded children.
   * The element name is this prefix plus the child field's name (e.g. loadedChildren+'orderLines').
   */
  public static final String SETTINGS_LOADED_CHILDREN_PREFIX = "loadedChildren";

  /**
   * Determine the repository associated with the given domain class. This is not for public access.
   * This is used only in the code inserted into the domain by the @DomainEntity annotation.
   *
   * @param clazz The domain class to check for a repository.
   * @return The repository bean.
   */
  GenericRepository determineRepository(Class clazz) throws ClassNotFoundException {
    Class<?> repoClazz = getRepositoryFromAnnotation(clazz);
    if (repoClazz == Object.class) {
      String className = clazz.getName() + "Repository";
      repoClazz = Class.forName(className);
    }

    return (GenericRepository) getApplicationContext().getBean(repoClazz);
  }

  /**
   * Determine the repository from the @DomainEntity annotation.
   *
   * @param clazz The domain class to get the repository for.
   * @return The repository class.
   */
  protected Class getRepositoryFromAnnotation(Class clazz) {
    DomainEntity annotation = (DomainEntity) clazz.getAnnotation(DomainEntity.class);
    if (annotation != null) {
      return annotation.repository();
    } else {
      return null;
    }
  }

  /**
   * Saves the given record.  If the record is new, this does an insert.  If not new, then it updates the record).
   * If the object has a uuid then assumes an update() is needed.
   * The repository must be a CrudRepository.
   *
   * @param object The domain object to save.
   * @return The object after saving.
   */
  Object save(DomainEntityInterface object) throws IllegalAccessException, NoSuchFieldException, InstantiationException, SQLException {
    CrudRepository repo = (CrudRepository) getRepository(object.getClass());
    if (repo == null) {
      throw new IllegalArgumentException("Missing repository for " + object.getClass());
    }
    if (object.getUuid() == null) {
      repo.save(object);
    } else {
      repo.update(object);
    }
    saveManyToMany(object);
    saveChildren(object);

    return object;
  }

  /**
   * Deletes the given record.
   *
   * @param object The domain object to delete.
   * @return The object that was deleted.
   */
  Object delete(DomainEntityInterface object) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, SQLException, InstantiationException {
    GenericRepository<DomainEntityInterface, UUID> repo = getRepository(object.getClass());
    if (repo == null) {
      throw new IllegalArgumentException("Missing repository for " + object.getClass());
    }
    if (repo instanceof CrudRepository) {
      CrudRepository<DomainEntityInterface, UUID> repo2 = (CrudRepository<DomainEntityInterface, UUID>) repo;
      repo2.delete(object);
    }
    deleteAllManyToMany(object);
    deleteChildren(object);

    return object;
  }

  /**
   * Gets the repository from the domain class (static) field.
   *
   * @param clazz The domain class to get the repo for.
   * @return The repository.
   */
  @SuppressWarnings("unchecked")
  GenericRepository<DomainEntityInterface, UUID> getRepository(Class clazz) {
    if (clazz != null) {
      try {
        //Class[] args = {};
        Method method = ((Class<?>) clazz).getMethod("getRepository");
        if (method != null) {
          Object res = method.invoke(null);
          if (res instanceof GenericRepository) {
            return (GenericRepository<DomainEntityInterface, UUID>) res;
          }
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        // Intentionally ignored.
        //ignored.printStackTrace();
      }
    }

    return null;
  }


  /**
   * Executes the list() method on the domain object's repository.
   *
   * @param domainClazz The domain class to read from.
   * @return The list.
   */
  public List<Object> list(Class domainClazz) {
    CrudRepository repo = (CrudRepository) getRepository(domainClazz);
    Iterable iter = repo.findAll();
    List<Object> list = new ArrayList<>();
    for (Object record : iter) {
      list.add(record);
    }
    return list;
  }


  /**
   * Handles missing static methods in a @DomainEntity.  Provides a link to the repository's findByXYZ() methods.
   *
   * @param domainClazz The domain class the static method is called from.
   * @param methodName  The method name.
   * @param args        The arguments for the method.
   * @return The results of the method call, or throws an exception if missing.
   */
  @SuppressWarnings("unused")
  public Object staticMethodMissingHandler(Class domainClazz, String methodName, Object[] args)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    //System.out.println("domainClazz:" + domainClazz+" method: "+methodName+" args:"+args.length);
    //System.out.println("  args:"+args.getClass());
    Class<?>[] paramTypes = null;
    if (args.length > 0) {
      paramTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        paramTypes[i] = args[i].getClass();
      }
      //System.out.println("paramTypes:" + paramTypes[0]);
    }
    CrudRepository repo = (CrudRepository) getRepository(domainClazz);
    Method method = repo.getClass().getDeclaredMethod(methodName, paramTypes);
    // For some reason, the class generated by Micronaut-data creates the class with protected packaging.
    // We need to make this method accessible for this invocation.
    method.setAccessible(true);

    Object res = method.invoke(repo, args);
    if (methodName.startsWith("findBy") && res instanceof Optional) {
      // Strip the Optional wrapper for the findBy() case.
      res = ((Optional<?>) res).orElse(null);
    }
    return res;
  }

  /**
   * A cached context.
   */
  ApplicationContext applicationContext;

  /**
   * Get the application context from the holders.
   *
   * @return The context.
   */
  public ApplicationContext getApplicationContext() {
    if (applicationContext == null) {
      // Use reflection to access the Holders.getApplicationContext() at run time since the Groovy
      // classes are not visible when this .java class is compiled.
      try {
        Class holdersClass = Class.forName("org.simplemes.eframe.application.Holders");
        Method method = ((Class<?>) holdersClass).getMethod("getApplicationContext");
        if (method != null) {
          applicationContext = (ApplicationContext) method.invoke(null);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException ignored) {
        // Intentionally ignored.
        //ignored.printStackTrace();
      }

    }
    return applicationContext;
  }

  /**
   * Get the current transaction manager.
   *
   * @return The transaction manager.
   */
  public SynchronousTransactionManager getTransactionManager() {
    return getApplicationContext().getBean(SynchronousTransactionManager.class);
  }

  /**
   * Start a transaction and rollback when finished.
   */
  @SuppressWarnings({"unchecked", "unused"})
  public Object executeWrite(TransactionCallback closure) {
    SynchronousTransactionManager manager = getTransactionManager();
    return manager.executeWrite(closure);
  }

  /**
   * Start a transaction and rollback when finished.
   */
  @SuppressWarnings({"unchecked", "unused"})
  public Object executeWriteClosure(Class delegate, Closure<Object> closure) {
    SynchronousTransactionManager manager = getTransactionManager();
    TransactionCallbackWrapper callback = new TransactionCallbackWrapper(closure);
    return manager.executeWrite(callback);
  }


  /**
   * Save the children for the given parent object.
   *
   * @param object The parent domain object.
   */
  @SuppressWarnings("unchecked")
  void saveChildren(DomainEntityInterface object) throws IllegalAccessException, NoSuchFieldException, InstantiationException {
    // Check all properties for (List) child properties.
    Map domainSettings = getDomainSettings(object);
    for (Field field : object.getClass().getDeclaredFields()) {
      // Performance: Consider moving the reflection logic to the Transformation to avoid run-time cost.
      OneToMany ann = field.getAnnotation(OneToMany.class);
      if (ann != null && Collection.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);  // Need to bypass the getter, since that would trigger a read in some cases.
        Collection list = (Collection) field.get(object);
        String mappedByFieldName = ann.mappedBy();
        Class<DomainEntityInterface> childClass = (Class<DomainEntityInterface>) getGenericType(field);
        //System.out.println("childClass:" + childClass);
        Field parentField = childClass.getDeclaredField(mappedByFieldName);
        parentField.setAccessible(true);  // Need to bypass any setters.
        //System.out.println("  parentField:" + parentField+" ");
        if (list != null) {
          List<Object> newRecordList = new ArrayList();
          for (Object child : list) {
            if (child instanceof DomainEntityInterface && mappedByFieldName.length() > 0) {
              //System.out.println("  child:" + child);
              // Make sure the parent element is set before the save.
              if (parentField.get(child) == null) {
                parentField.set(child, object);
              }
              ((DomainEntityInterface) child).save();
              newRecordList.add(((DomainEntityInterface) child).getUuid());
            }
          }
          // Now, delete any removed records from the previous read.
          if (domainSettings != null) {
            List<UUID> previouslyLoadedList = (List) domainSettings.get(SETTINGS_LOADED_CHILDREN_PREFIX + field.getName());

            for (UUID uuid : previouslyLoadedList) {
              if (!newRecordList.contains(uuid)) {
                // The child record is no longer in the ths list, so delete it.
                DomainEntityInterface childObject = childClass.newInstance();
                childObject.setUuid(uuid);
                childObject.delete();
              }
            }
          }

          // Finally, use the new list of updated records.
          List loadedList = new ArrayList();
          if (domainSettings != null) {
            domainSettings.put(SETTINGS_LOADED_CHILDREN_PREFIX + field.getName(), loadedList);
            for (Object child : list) {
              if (child instanceof DomainEntityInterface) {
                loadedList.add(((DomainEntityInterface) child).getUuid());
              }
            }
          }
        }
      }
    }
  }

  /**
   * Deletes the children for the given parent object.
   *
   * @param object The parent domain object.
   */
  void deleteChildren(DomainEntityInterface object) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    // Check all properties for (List) child properties.
    for (Field field : object.getClass().getDeclaredFields()) {
      OneToMany ann = field.getAnnotation(OneToMany.class);
      if (ann != null && Collection.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);  // Need to bypass the getter, since that would trigger a read in some cases.
        field.set(object, null);
        Method getterMethod = object.getClass().getMethod("get" + StringUtils.capitalize(field.getName()));
        Collection list = (Collection) getterMethod.invoke(object);
        if (list != null) {
          for (Object child : list) {
            if (child instanceof DomainEntityInterface) {
              ((DomainEntityInterface) child).delete();
            }
          }
        }
      }
    }
  }

  /**
   * Save the many-to-many references.
   *
   * @param object The parent domain object.
   */
  @SuppressWarnings("unchecked")
  void saveManyToMany(DomainEntityInterface object) throws IllegalAccessException, InstantiationException, SQLException {
    for (Field field : object.getClass().getDeclaredFields()) {
      // Performance: Consider moving the reflection logic to the Transformation to avoid run-time cost.
      ManyToMany ann = field.getAnnotation(ManyToMany.class);
      if (ann != null && Collection.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);  // Need to bypass the getter, since that would trigger a read in some cases.
        Collection list = (Collection) field.get(object);
        String tableName = getNamingStrategy(object).mappedName(ann.mappedBy());
        Class<DomainEntityInterface> childClass = (Class<DomainEntityInterface>) getGenericType(field);
        String fromIDName = getNamingStrategy(object).mappedName(object.getClass().getSimpleName()) + "_id";
        String toIDName = getNamingStrategy(object).mappedName(childClass.getSimpleName()) + "_id";
        // Remove current list.
        deleteAllManyToMany(object.getUuid(), tableName, fromIDName);
        // Performance: Consider storing the list of records from previous read and just doing a specific delete.
        if (list != null) {
          for (Object child : list) {
            if (child instanceof DomainEntityInterface && ann.mappedBy().length() > 0) {
              // Make sure the parent element is set before the save.
              String sql = "INSERT INTO " + tableName + " (" + fromIDName + "," + toIDName + ") VALUES (?,?)";

              PreparedStatement ps = getPreparedStatement(sql);
              ps.setString(1, object.getUuid().toString());
              ps.setString(2, ((DomainEntityInterface) child).getUuid().toString());
              ps.execute();

              ((DomainEntityInterface) child).save();
            }
          }
        }
      }
    }
  }

  /**
   * Delete all many-to-many lists from the DB.
   *
   * @param object The domain object.
   */
  private void deleteAllManyToMany(DomainEntityInterface object) throws SQLException, IllegalAccessException, InstantiationException {
    for (Field field : object.getClass().getDeclaredFields()) {
      // Performance: Consider moving the reflection logic to the Transformation to avoid run-time cost.
      ManyToMany ann = field.getAnnotation(ManyToMany.class);
      if (ann != null && Collection.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);  // Need to bypass the getter, since that would trigger a read in some cases.
        String tableName = getNamingStrategy(object).mappedName(ann.mappedBy());
        String fromIDName = getNamingStrategy(object).mappedName(object.getClass().getSimpleName()) + "_id";
        // Remove current records.
        deleteAllManyToMany(object.getUuid(), tableName, fromIDName);
      }
    }
  }

  /**
   * Deletes all records in the many to many JOIN table.
   *
   * @param uuid       The parent record.
   * @param tableName  The table
   * @param fromIDName The parent column name
   */
  private void deleteAllManyToMany(UUID uuid, String tableName, String fromIDName) throws SQLException {
    String sql = "DELETE FROM " + tableName + " WHERE " + fromIDName + "=?";
    PreparedStatement ps = getPreparedStatement(sql);
    ps.setString(1, uuid.toString());
    ps.execute();

  }

  /**
   * Creates a prepared SQL statement.
   *
   * @param sql The SQL.
   * @return The statement.
   */
  private PreparedStatement getPreparedStatement(String sql) throws SQLException {
    DataSource dataSource = getApplicationContext().getBean(DataSource.class);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    return connection.prepareStatement(sql);
  }

  /**
   * Returns the naming strategy in use for the given Mapped entity.
   *
   * @param object The domain object (a @MappedEntity).
   * @return The strategy.
   */
  NamingStrategy getNamingStrategy(DomainEntityInterface object) throws IllegalAccessException, InstantiationException {
    Class<? extends NamingStrategy> namingStrategy = object.getClass().getAnnotation(MappedEntity.class).namingStrategy();
    return namingStrategy.newInstance();
  }


  /**
   * Gets the (first) generic type for the given field.  Works for fields like: List&lt;XYZ&gt;.
   *
   * @param field The field.
   * @return The underlying type (from the generic definition).  Can be null.
   */
  Class<?> getGenericType(Field field) {
    Type childType = field.getGenericType();
    if (!(childType instanceof ParameterizedType)) {
      return null;
    }
    ParameterizedType childParameterizedType = (ParameterizedType) childType;
    return (Class<?>) childParameterizedType.getActualTypeArguments()[0];
  }


  /**
   * Performs the lazy load of the given field from the child domain class using the given mapped by
   * field name.  Calls the findAllByXYZ() method on the child repository.
   * After the list is first read, it will be saved in the field and re-used on later calls to the loader.
   *
   * @param object            The parent domain object to load the child from.
   * @param fieldName         The field to store the list in.  Used by later calls.
   * @param mappedByFieldName The field in the child that references the parent element.
   * @param childDomainClazz  The child domain class.
   * @return The list.
   */
  @SuppressWarnings("unchecked")
  public List lazyChildLoad(DomainEntityInterface object, String fieldName, String mappedByFieldName, Class childDomainClazz)
      throws Throwable {
    //System.out.println("object:" + object+" fieldName:" + fieldName+" mappedByFieldName:" + mappedByFieldName+" childDomainClazz:" + childDomainClazz);
    // Find the current value.  Use reflection to access the field, even if not public.
    Field field = object.getClass().getDeclaredField(fieldName);
    //System.out.println("field:" + field);
    field.setAccessible(true);  // Allow direct access.
    List list = (List) field.get(object);
    if (list == null) {
      // Set the list to empty to avoid stack overflow in case of exception calling the getter over and over.
      // This happens when the parent object is not saved yet.
      list = new ArrayList();
      field.set(object, list);
      Map domainSettings = getDomainSettings(object);
      List loadedList = new ArrayList();
      if (domainSettings != null) {
        domainSettings.put(SETTINGS_LOADED_CHILDREN_PREFIX + fieldName, loadedList);
      }
      GenericRepository repository = getRepository(childDomainClazz);
      UUID uuid = object.getUuid();
      if (repository != null && uuid != null) {
        String finderName = "findAllBy" + StringUtils.capitalize(mappedByFieldName);
        Method method = repository.getClass().getMethod(finderName, object.getClass());
        method.setAccessible(true);  // For some reason, Micronaut-data creates the method that is not accessible.
        try {
          list = (List) method.invoke(repository, object);
          field.set(object, list);
          // Store the UUID's for later checks on update.
          for (Object child : list) {
            if (child instanceof DomainEntityInterface) {
              loadedList.add(((DomainEntityInterface) child).getUuid());
            }
          }
        } catch (Throwable e) {
          // Most exceptions are wrapped in invocation target exceptions, so we can remove them.
          throw unwrapException(e);
        }
      }
    }

    return list;
  }

  /**
   * Finds the domain settings holder for given domain object.
   *
   * @param object The domain object.
   * @return The settings holder.
   */
  private Map<?, ?> getDomainSettings(DomainEntityInterface object) throws IllegalAccessException, NoSuchFieldException {
    // Uses reflection since the element is a field and that is difficult to implement in an interface.
    Field field = object.getClass().getDeclaredField(DOMAIN_SETTINGS_FIELD_NAME);
    Object o = field.get(object);
    if (o instanceof Map) {
      return (Map) o;
    }
    return null;
  }

  /**
   * Unwraps the given exception to find the root cause.  This unwraps  InvocationTargetException and
   * UndeclaredThrowableException to find the real cause (SQL or micronaut exception).
   *
   * @param e The exception to unwrap.
   * @return The unwrapped exception or the original exception if not unwrap-able.
   */
  Throwable unwrapException(Throwable e) {
    if (e instanceof UndeclaredThrowableException) {
      e = e.getCause();
    }
    if (e instanceof InvocationTargetException) {
      e = e.getCause();
    }
    return e;
  }

  /**
   * Performs the validations on the given domain object and returns a list of errors related to the problem.
   *
   * @param object The domain object.
   * @return The list of validation errors.  Never null.
   */
  List<ValidationErrorInterface> validate(DomainEntityInterface object) throws InvocationTargetException, IllegalAccessException {
    List<ValidationErrorInterface> res = validateColumns(object);
    try {
      Method validateMethod = object.getClass().getDeclaredMethod("validate");
      Object methodRes = validateMethod.invoke(object);
      if (methodRes instanceof ValidationErrorInterface) {
        res.add((ValidationErrorInterface) methodRes);
      } else if (methodRes instanceof Collection<?>) {
        for (Object error : (Collection<?>) methodRes) {
          if (error instanceof ValidationErrorInterface) {
            res.add((ValidationErrorInterface) error);
          }
        }
      } else {
        throw new IllegalArgumentException(object.getClass().getName() + ".validate() must return a ValidationErrorInterface or list.");
      }
    } catch (NoSuchMethodException ignored) {
      //e.printStackTrace();
    }

    return res;
  }

  /**
   * A list of field names to ignore for the internal validations (e.g. null and length).
   */
  String[] ignoreFieldsForValidation = {"uuid", "dateCreated", "dateUpdated"};

  /**
   * Determines if the given field should be ignored from the core validations (e.g. null and length).
   *
   * @param fieldName The field to check.
   * @return True if the field should be ignored.
   */
  protected boolean ignoreFieldForValidation(String fieldName) {
    for (String s : ignoreFieldsForValidation) {
      if (s.equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Performs the validations on the given domain object and returns a list of errors related to the problem.
   *
   * @param object The domain object.
   * @return The list of validation errors.  Never null.
   */
  protected List<ValidationErrorInterface> validateColumns(DomainEntityInterface object) {
    List<ValidationErrorInterface> res = new ArrayList<>();
    for (PersistentProperty property : getPersistentProperties(object.getClass())) {
      if (!ignoreFieldForValidation(property.getName())) {
        if (!property.isNullable()) {
          try {
            property.getField().setAccessible(true);  // Use work around to make this code simpler.  Should call getter().
            Object value = property.getField().get(object);
            if (value == null) {
              //error.1.message=Required value is missing {0}.
              res.add(new ValidationError(1, property.getName()));
            }
          } catch (IllegalAccessException ignored) {
            // Will always be accessible.
          }
        }
        if (property.getType() == String.class && property.getMaxLength() > 0) {
          try {
            property.getField().setAccessible(true);  // Use work around to make this code simpler.  Should call getter().
            String value = (String) property.getField().get(object);
            if (value != null) {
              if (value.length() > property.getMaxLength()) {
                //error.2.message=Value is too long (max={2}, length={1}) for field {0}.
                res.add(new ValidationError(2, property.getName(), value.length(), property.getMaxLength()));
              }
            }
          } catch (IllegalAccessException ignored) {
            // Will always be accessible.
          }
        }
      }
    }
    return res;
  }

  /**
   * Returns the list of persistent properties (columns) for the given domain class.
   *
   * @param domainClass The domain class.
   * @return The list of persistent properties.  Never null.
   */
  public List<PersistentProperty> getPersistentProperties(Class domainClass) {
    List<PersistentProperty> res = new ArrayList<>();
    for (Field field : domainClass.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
        // Weed out any fields the use @Transient too
        io.micronaut.data.annotation.Transient transAnn = field.getAnnotation(io.micronaut.data.annotation.Transient.class);
        if (transAnn == null) {
          res.add(new PersistentProperty(field));
        }
      }
    }
    return res;
  }


  public static DomainEntityHelper getInstance() {
    return instance;
  }

  public static void setInstance(DomainEntityHelper instance) {
    DomainEntityHelper.instance = instance;
  }

  /**
   * Local class used to call the closure from the as a TransactionCallback.
   */
  protected static class TransactionCallbackWrapper implements TransactionCallback {

    Closure<Object> closure;

    public TransactionCallbackWrapper(Closure<Object> closure) {
      this.closure = closure;
    }

    /**
     * Code that runs within the context of a transaction will implement this method.
     *
     * @param status The transaction status.
     * @return The return value
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @Override
    public Object call(@edu.umd.cs.findbugs.annotations.NonNull TransactionStatus status) {
      return closure.call(status);
    }

    /**
     * Applies this function to the given argument.
     *
     * @param o the function argument
     * @return the function result
     */
    @Override
    public Object apply(Object o) {
      return null;
    }
  }
}
