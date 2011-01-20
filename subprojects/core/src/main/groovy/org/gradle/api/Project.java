/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api;

import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.artifacts.dsl.*;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.WorkResult;
import org.gradle.process.ExecResult;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>This interface is the main API you use to interact with Gradle from your build file. From a <code>Project</code>,
 * you have programmatic access to all of Gradle's features.</p>
 *
 * <h3>Lifecycle</h3>
 *
 * <p>There is a one-to-one relationship between a <code>Project</code> and a <code>{@value #DEFAULT_BUILD_FILE}</code>
 * file. During build initialisation, Gradle assembles a <code>Project</code> object for each project which is to
 * participate in the build, as follows:</p>
 *
 * <ul>
 *
 * <li>Create a {@link org.gradle.api.initialization.Settings} instance for the build.</li>
 *
 * <li>Evaluate the <code>{@value org.gradle.api.initialization.Settings#DEFAULT_SETTINGS_FILE}</code> script, if
 * present, against the {@link org.gradle.api.initialization.Settings} object to configure it.</li>
 *
 * <li>Use the configured {@link org.gradle.api.initialization.Settings} object to create the hierarchy of
 * <code>Project</code> instances.</li>
 *
 * <li>Finally, evaluate each <code>Project</code> by executing its <code>{@value #DEFAULT_BUILD_FILE}</code> file, if
 * present, against the project. The project are evaulated in breadth-wise order, such that a project is evaulated
 * before its child projects. This order can be overridden by adding an evaluation dependency.</li>
 *
 * </ul>
 *
 * <h3>Tasks</h3>
 *
 * <p>A project is essentially a collection of {@link Task} objects. Each task performs some basic piece of work, such
 * as compiling classes, or running unit tests, or zipping up a WAR file. You add tasks to a project using one of the
 * {@code add()} methods on {@link TaskContainer}, such as {@link TaskContainer#add(String)}.  You can locate existing
 * tasks using one of the lookup methods on {@link TaskContainer}, such as {@link org.gradle.api.tasks.TaskCollection#getByName(String)}.</p>
 *
 * <h3>Dependencies</h3>
 *
 * <p>A project generally has a number of dependencies it needs in order to do its work.  Also, a project generally
 * produces a number of artifacts, which other projects can use. Those dependencies are grouped in configurations, and
 * can be retrieved and uploaded from repositories. You use the {@link org.gradle.api.artifacts.ConfigurationContainer}
 * returned by {@link #getConfigurations()} method to manage the configurations. The {@link
 * org.gradle.api.artifacts.dsl.DependencyHandler} returned by {@link #getDependencies()} method to manage the
 * dependencies. The {@link org.gradle.api.artifacts.dsl.ArtifactHandler} returned by {@link #getArtifacts()} method to
 * manage the artifacts. The {@link org.gradle.api.artifacts.dsl.RepositoryHandler} returned by {@link
 * #getRepositories()} method to manage the repositories.</p>
 *
 * <h3>Multi-project Builds</h3>
 *
 * <p>Projects are arranged into a hierarchy of projects. A project has a name, and a fully qualified path which
 * uniquely identifies it in the hierarchy.</p>
 *
 * <h3>Build scripts</h3>
 *
 * <p>Gradle executes the project's build file against the <code>Project</code> instance to configure the project. Any
 * property or method which your script uses which is not defined in the script is delegated through to the associated
 * <code>Project</code> object.  This means, that you can use any of the methods and properties on the
 * <code>Project</code> interface directly in your script.</p><p>For example:
 * <pre>
 * defaultTasks('some-task')  // Delegates to Project.defaultTasks()
 * reportDir = file('reports') // Delegates to Project.file() and Project.setProperty()
 * </pre>
 * <p>You can also access the <code>Project</code> instance using the <code>project</code> property. This can make the
 * script clearer in some cases. For example, you could use <code>project.name</code> rather than <code>name</code> to
 * access the project's name.</p>
 *
 * <a name="properties"/> <h4>Dynamic Properties</h4>
 *
 * <p>A project has 5 property 'scopes', which it searches for properties. You can access these properties by name in
 * your build file, or by calling the project's {@link #property(String)} method. The scopes are:</p>
 *
 * <ul>
 *
 * <li>The <code>Project</code> object itself. This scope includes any property getters and setters declared by the
 * <code>Project</code> implementation class.  For example, {@link #getRootProject()} is accessible as the
 * <code>rootProject</code> property.  The properties of this scope are readable or writable depending on the presence
 * of the corresponding getter or setter method.</li>
 *
 * <li>The <em>additional</em> properties of the project.  Each project maintains a map of additional properties, which
 * can contain any arbitrary name -> value pair.  The properties of this scope are readable and writable.</li>
 *
 * <li>The <em>convention</em> properties added to the project by each {@link Plugin} applied to the project. A {@link
 * Plugin} can add properties and methods to a project through the project's {@link Convention} object.  The properties
 * of this scope may be readable or writable, depending on the convention objects.</li>
 *
 * <li>The tasks of the project.  A task is accessible by using its name as a property name.  The properties of this
 * scope are read-only. For example, a task called <code>compile</code> is accessible as the <code>compile</code>
 * property.</li>
 *
 * <li>The additional properties and convention properties of the project's parent project, recursively up to the root
 * project. The properties of this scope are read-only.</li>
 *
 * </ul>
 *
 * <p>When reading a property, the project searches the above scopes in order, and returns the value from the first
 * scope it finds the property in.  See {@link #property(String)} for more details.</p>
 *
 * <p>When writing a property, the project searches the above scopes in order, and sets the property in the first scope
 * it finds the property in. If not found, the project adds the property to its map of additional properties. See {@link
 * #setProperty(String, Object)} for more details.</p>
 *
 * <h4>Dynamic Methods</h4>
 *
 * <p>A project has 5 method 'scopes', which it searches for methods:</p>
 *
 * <ul>
 *
 * <li>The <code>Project</code> object itself.</li>
 *
 * <li>The build file. The project searches for a matching method declared in the build file.</li>
 *
 * <li>The <em>convention</em> methods added to the project by each {@link Plugin} applied to the project. A {@link
 * Plugin} can add properties and method to a project through the project's {@link Convention} object.</li>
 *
 * <li>The tasks of the project. A method is added for each task, using the name of the task as the method name and
 * taking a single closure parameter. The method calls the {@link Task#configure(groovy.lang.Closure)} method for the
 * associated task with the provided closure. For example, if the project has a task called <code>compile</code>, then a
 * method is added with the following signature: <code>void compile(Closure configureClosure)</code>.</li>
 *
 * <li>The parent project, recursively up to the root project.</li>
 *
 * </ul>
 *
 * @author Hans Dockter
 */
public interface Project extends Comparable<Project> {
    /**
     * The default project build file name.
     */
    public static final String DEFAULT_BUILD_FILE = "build.gradle";

    /**
     * The hierarchy separator for project and task path names.
     */
    public static final String PATH_SEPARATOR = ":";

    /**
     * The default build directory name.
     */
    public static final String DEFAULT_BUILD_DIR_NAME = "build";

    public static final String GRADLE_PROPERTIES = "gradle.properties";

    public static final String SYSTEM_PROP_PREFIX = "systemProp";

    public static final String TMP_DIR_NAME = ".gradle";

    public static final String DEFAULT_VERSION = "unspecified";

    public static final String DEFAULT_STATUS = "release";

    /**
     * <p>Returns the root project for the hierarchy that this project belongs to.  In the case of a single-project
     * build, this method returns this project.</p>
     *
     * @return The root project. Never returns null.
     */
    Project getRootProject();

    /**
     * <p>Returns the root directory of this project. The root directory is the project directory of the root
     * project.</p>
     *
     * @return The root directory. Never returns null.
     */
    File getRootDir();

    /**
     * <p>Returns the build directory of this project.  The build directory is the directory which all artifacts are
     * generated into.  The default value for the build directory is <code><i>projectDir</i>/build</code></p>
     *
     * @return The build directory. Never returns null.
     */
    File getBuildDir();

    /**
     * <p>Sets the build directory of this project. The build directory is the directory which all artifacts are
     * generated into. The path parameter is evaluated as described for {@link #file(Object)}. This mean you can use,
     * amongst other things, a relative or absolute path or File object to specify the build directory.</p>
     *
     * @param path The build directory. This is evaluated as for {@link #file(Object)}
     */
    void setBuildDir(Object path);

    /**
     * <p>Returns the name of the build directory of this project. It is resolved relative to the project directory of
     * this project to determine the build directory. The default value is {@value #DEFAULT_BUILD_DIR_NAME}.</p>
     *
     * @return The build dir name. Never returns null.
     */
    @Deprecated
    String getBuildDirName();

    /**
     * <p>Sets the build directory name of this project.</p>
     *
     * @param buildDirName The build dir name. Should not be null.
     * @deprecated Use {@link #setBuildDir(Object)} instead.
     */
    @Deprecated
    void setBuildDirName(String buildDirName);

    /**
     * <p>Returns the build file Gradle will evaluate against this project object. The default is <code> {@value
     * #DEFAULT_BUILD_FILE}</code>. If an embedded script is provided the build file will be null. </p>
     *
     * @return Current build file. May return null.
     */
    File getBuildFile();

    /**
     * <p>Returns the parent project of this project, if any.</p>
     *
     * @return The parent project, or null if this is the root project.
     */
    Project getParent();

    /**
     * <p>Returns the name of this project. The project's name is not necessarily unique within a project hierarchy. You
     * should use the {@link #getPath()} method for a unique identifier for the project.</p>
     *
     * @return The name of this project. Never return null.
     */
    String getName();

    /**
     * Returns the description of this project.
     *
     * @return the description. May return null.
     */
    String getDescription();

    /**
     * Sets a description for this project.
     *
     * @param description The description of the project. Might be null.
     */
    void setDescription(String description);

    /**
     * <p>Returns the group of this project. Gradle always uses the {@code toString()} value of the group. The group
     * defaults to the path with dots a separators.</p>
     *
     * @return The group of this project. Never returns null.
     */
    Object getGroup();

    /**
     * <p>Sets the group of this project.</p>
     *
     * @param group The group of this project. Must not be null.
     */
    void setGroup(Object group);

    /**
     * <p>Returns the version of this project. Gradle always uses the {@code toString()} value of the version. The
     * version defaults to {@value #DEFAULT_VERSION}.</p>
     *
     * @return The version of this project. Never returns null.
     */
    Object getVersion();

    /**
     * <p>Sets the version of this project.</p>
     *
     * @param version The version of this project. Must not be null.
     */
    void setVersion(Object version);

    /**
     * <p>Returns the status of this project. Gradle always uses the {@code toString()} value of the status. The status
     * defaults to {@value #DEFAULT_STATUS}.</p>
     *
     * <p>The status of the project is only relevant, if you upload libraries together with a module descriptor. The
     * status specified here, will be part of this module descriptor.</p>
     *
     * @return The status of this project. Never returns null.
     */
    Object getStatus();

    /**
     * Sets the status of this project.
     *
     * @param status The status. Must not be null.
     */
    void setStatus(Object status);

    /**
     * <p>Returns the direct children of this project.</p>
     *
     * @return A map from child project name to child project. Returns an empty map if this this project does not have
     *         any children.
     */
    Map<String, Project> getChildProjects();

    /**
     * <p>Returns the set of projects which this project depends on.</p>
     *
     * @return The set of projects. Returns an empty set if this project depends on no projects.
     */
    Set<Project> getDependsOnProjects();

    /**
     * <p>Sets a property of this project.  This method searches for a property with the given name in the following
     * locations, and sets the property on the first location where it finds the property.</p>
     *
     * <ol>
     *
     * <li>The project object itself.  For example, the <code>rootDir</code> project property.</li>
     *
     * <li>The project's {@link Convention} object.  For example, the <code>srcRootName</code> java plugin
     * property.</li>
     *
     * <li>The project's additional properties.</li>
     *
     * </ol>
     *
     * <p>If the property is not found in any of these locations, it is added to the project's additional
     * properties.</p>
     *
     * @param name The name of the property
     * @param value The value of the property
     */
    void setProperty(String name, Object value);

    /**
     * <p>Returns this project. This method is useful in build files to explicitly access project properties and
     * methods. For example, using <code>project.name</code> can express your intent better than using
     * <code>name</code>. This method also allows you to access project properties from a scope where the property may
     * be hidden, such as, for example, from a method or closure. </p>
     *
     * @return This project. Never returns null.
     */
    Project getProject();

    /**
     * <p>Returns the set containing this project and its subprojects.</p>
     *
     * @return The set of projects.
     */
    Set<Project> getAllprojects();

    /**
     * <p>Returns the set containing the subprojects of this project.</p>
     *
     * @return The set of projects.  Returns an empty set if this project has no subprojects.
     */
    Set<Project> getSubprojects();

    /**
     * <p>Applies a {@link Plugin} to this project.</p>
     *
     * @param pluginId The id of the plugin.
     * @return This project.
     * @deprecated You should use the {@link #apply(java.util.Map)} or {@link #apply(groovy.lang.Closure)} method
     *             instead.
     */
    @Deprecated
    Project usePlugin(String pluginId);

    /**
     * <p>Applies a {@link Plugin} to this project.</p>
     *
     * @param pluginClass The class of the plugin.  This class must implement the {@link Plugin} interface.
     * @return This project.
     * @deprecated You should use the {@link #apply(java.util.Map)} or {@link #apply(groovy.lang.Closure)} method
     *             instead.
     */
    @Deprecated
    Project usePlugin(Class<? extends Plugin> pluginClass);

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Calling this method is equivalent to
     * calling {@link #task(java.util.Map, String)} with an empty options map.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project, an exception is thrown.</p>
     *
     * @param name The name of the task to be created
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     */
    Task task(String name) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. A map of creation options can be
     * passed to this method to control how the task is created. The following options are available:</p>
     *
     * <table>
     *
     * <tr><th>Option</th><th>Description</th><th>Default Value</th></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_TYPE}</code></td><td>The class of the task to
     * create.</td><td>{@link org.gradle.api.DefaultTask}</td></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_OVERWRITE}</code></td><td>Replace an existing
     * task?</td><td><code>false</code></td></tr>
     *
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_DEPENDS_ON}</code></td><td>A task name or set of task names which
     * this task depends on</td><td><code>[]</code></td></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_ACTION}</code></td><td>A closure or {@link Action} to add to the
     * task.</td><td><code>null</code></td></tr>
     *
     * </table>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project and the <code>override</code> option is not set
     * to true, an exception is thrown.</p>
     *
     * @param args The task creation options.
     * @param name The name of the task to be created
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     */
    Task task(Map<String, ?> args, String name) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * closure is executed to configure the task. A map of creation options can be passed to this method to control how
     * the task is created. See {@link #task(java.util.Map, String)} for the available options.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project and the <code>override</code> option is not set
     * to true, an exception is thrown.</p>
     *
     * @param args The task creation options.
     * @param name The name of the task to be created
     * @param configureClosure The closure to use to configure the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     */
    Task task(Map<String, ?> args, String name, Closure configureClosure);

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * closure is executed to configure the task.</p> <p/> <p>After the task is added to the project, it is made
     * available as a property of the project, so that you can reference the task by name in your build file.  See <a
     * href="#properties">here</a> for more details</p>
     *
     * @param name The name of the task to be created
     * @param configureClosure The closure to use to configure the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     */
    Task task(String name, Closure configureClosure);

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Calling this method is equivalent to
     * calling {@link #createTask(java.util.Map, String)} with an empty options map.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project, an exception is thrown.</p>
     *
     * @param name The name of the task to be created
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(String)} instead.
     */
    @Deprecated
    Task createTask(String name) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * action is passed to the task's {@link Task#doFirst(Action)} method. Calling this method is equivalent to calling
     * {@link #createTask(java.util.Map, String, Action)} with an empty options map.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project, an exception is thrown.</p>
     *
     * @param name The name of the task to be created
     * @param action The action to be passed to the {@link Task#doFirst(Action)} method of the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(java.util.Map, String)} instead.
     */
    @Deprecated
    Task createTask(String name, Action<? super Task> action) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. A map of creation options can be
     * passed to this method to control how the task is created. The following options are available:</p>
     *
     * <table>
     *
     * <tr><th>Option</th><th>Description</th><th>Default Value</th></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_TYPE}</code></td><td>The class of the task to
     * create.</td><td>{@link org.gradle.api.DefaultTask}</td></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_OVERWRITE}</code></td><td>Replace an existing
     * task?</td><td><code>false</code></td></tr>
     *
     * <tr><td><code>{@value org.gradle.api.Task#TASK_DEPENDS_ON}</code></td><td>A task name or set of task names which
     * this task depends on</td><td><code>[]</code></td></tr>
     *
     * </table>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project and the <code>override</code> option is not set
     * to true, an exception is thrown.</p>
     *
     * @param args The task creation options.
     * @param name The name of the task to be created
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(java.util.Map, String)} instead.
     */
    @Deprecated
    Task createTask(Map<String, ?> args, String name) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * action is passed to the task's {@link Task#doFirst(Action)} method. A map of creation options can be passed to
     * this method to control how the task is created. See {@link #createTask(java.util.Map, String)} for the available
     * options.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project and the <code>override</code> option is not set
     * to true, an exception is thrown.</p>
     *
     * @param args The task creation options.
     * @param name The name of the task to be created
     * @param action The action to be passed to the {@link Task#doFirst(Action)} method of the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(java.util.Map, String)} instead.
     */
    @Deprecated
    Task createTask(Map<String, ?> args, String name, Action<? super Task> action) throws InvalidUserDataException;

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * action closure is passed to the task's {@link Task#doFirst(Closure)} method. Calling this method is equivalent to
     * calling {@link #createTask(java.util.Map, String, Closure)} with an empty options map.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project, an exception is thrown.</p>
     *
     * @param name The name of the task to be created
     * @param action The closure to be passed to the {@link Task#doFirst(Closure)} method of the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(java.util.Map, String)} instead.
     */
    @Deprecated
    Task createTask(String name, Closure action);

    /**
     * <p>Creates a {@link Task} with the given name and adds it to this project. Before the task is returned, the given
     * action closure is passed to the task's {@link Task#doFirst(Closure)} method. A map of creation options can be
     * passed to this method to control how the task is created. See {@link #createTask(java.util.Map, String)} for the
     * available options.</p>
     *
     * <p>After the task is added to the project, it is made available as a property of the project, so that you can
     * reference the task by name in your build file.  See <a href="#properties">here</a> for more details</p>
     *
     * <p>If a task with the given name already exists in this project and the <code>override</code> option is not set
     * to true, an exception is thrown.</p>
     *
     * @param args The task creation options.
     * @param name The name of the task to be created
     * @param action The closure to be passed to the {@link Task#doFirst(Closure)} method of the created task.
     * @return The newly created task object
     * @throws InvalidUserDataException If a task with the given name already exists in this project.
     * @deprecated You should use {@link #task(java.util.Map, String)} instead.
     */
    @Deprecated
    Task createTask(Map<String, ?> args, String name, Closure action);

    /**
     * <p>Returns the path of this project.  The path is the fully qualified name of the project.</p>
     *
     * @return The path. Never returns null.
     */
    String getPath();

    /**
     * <p>Returns the names of the default tasks of this project. These are used when no tasks names are provided when
     * starting the build.</p>
     *
     * @return The default task names. Returns an empty list if this project has no default tasks.
     */
    List<String> getDefaultTasks();

    /**
     * <p>Sets the names of the default tasks of this project. These are used when no tasks names are provided when
     * starting the build.</p>
     *
     * @param defaultTasks The default task names.
     */
    void setDefaultTasks(List<String> defaultTasks);

    /**
     * <p>Sets the names of the default tasks of this project. These are used when no tasks names are provided when
     * starting the build.</p>
     *
     * @param defaultTasks The default task names.
     */
    void defaultTasks(String... defaultTasks);

    /**
     * <p>Declares that this project has an execution dependency on the project with the given path.</p>
     *
     * @param path The path of the project which this project depends on.
     * @throws UnknownProjectException If no project with the given path exists.
     */
    void dependsOn(String path) throws UnknownProjectException;

    /**
     * <p>Declares that this project has an execution dependency on the project with the given path.</p>
     *
     * @param path The path of the project which this project depends on.
     * @param evaluateDependsOnProject If true, adds an evaluation dependency.
     * @throws UnknownProjectException If no project with the given path exists.
     */
    void dependsOn(String path, boolean evaluateDependsOnProject) throws UnknownProjectException;

    /**
     * <p>Declares that this project has an evaulation dependency on the project with the given path.</p>
     *
     * @param path The path of the project which this project depends on.
     * @return The project which this project depends on.
     * @throws UnknownProjectException If no project with the given path exists.
     */
    Project evaluationDependsOn(String path) throws UnknownProjectException;

    /**
     * <p>Declares that all child projects of this project have an execution dependency on this project.</p>
     *
     * @return this project.
     */
    Project childrenDependOnMe();

    /**
     * <p>Declares that this project have an execution dependency on each of its child projects.</p>
     *
     * @return this project.
     */
    Project dependsOnChildren();

    /**
     * <p>Declares that this project have an execution dependency on each of its child projects.</p>
     *
     * @param evaluateDependsOnProject If true, adds an evaluation dependency.
     * @return this project.
     */
    Project dependsOnChildren(boolean evaluateDependsOnProject);

    /**
     * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
     *
     * @param path The path.
     * @return The project with the given path. Returns null if no such project exists.
     */
    Project findProject(String path);

    /**
     * <p>Locates a project by path. If the path is relative, it is interpreted relative to this project.</p>
     *
     * @param path The path.
     * @return The project with the given path. Never returns null.
     * @throws UnknownProjectException If no project with the given path exists.
     */
    Project project(String path) throws UnknownProjectException;

    /**
     * <p>Locates a project by path and configures it using the given closure. If the path is relative, it is
     * interpreted relative to this project. The target project is passed to the closure as the closure's delegate.</p>
     *
     * @param path The path.
     * @param configureClosure The closure to use to configure the project.
     * @return The project with the given path. Never returns null.
     * @throws UnknownProjectException If no project with the given path exists.
     */
    Project project(String path, Closure configureClosure);

    /**
     * <p>Returns a map of the tasks contained in this project, and optionally its subprojects.</p>
     *
     * @param recursive If true, returns the tasks of this project and its subprojects.  If false, returns the tasks of
     * just this project.
     * @return A map from project to a set of tasks.
     */
    Map<Project, Set<Task>> getAllTasks(boolean recursive);

    /**
     * <p>Returns the set of tasks with the given name contained in this project, and optionally its subprojects.</p>
     *
     * @param name The name of the task to locate.
     * @param recursive If true, returns the tasks of this project and its subprojects. If false, returns the tasks of
     * just this project.
     * @return The set of tasks. Returns an empty set if no such tasks exist in this project.
     */
    Set<Task> getTasksByName(String name, boolean recursive);

    /**
     * <p>The directory containing the project build file.</p>
     *
     * @return The project directory. Never returns null.
     */
    File getProjectDir();

    /**
     * <p>Resolves a file path relative to the project directory of this project. This method converts the supplied path
     * based on its type:</p>
     *
     * <ul>
     *
     * <li>{@link File}. If the file is an absolute file, it is returned as is. Otherwise, the file's path is
     * interpreted relative to the project directory.</li>
     *
     * <li>{@link java.net.URI} or {@link java.net.URL}. The URL's path is interpreted as the file path. Currently, only
     * {@code file:} URLs are supported.
     *
     * <li>{@link Closure}. The closure's return value is resolved recursively.</li>
     *
     * <li>{@link java.util.concurrent.Callable}. The callable's return value is resolved recursively.</li>
     *
     * <li>{@link Object}. The object's {@code toString()} value is interpreted as a path. If the path is a relative
     * path, the project directory will be used as a base directory. A String starting with {@code file:} is treated as
     * a file URL.</li>
     *
     * </ul>
     *
     * @param path The object to resolve as a File.
     * @return The resolved file. Never returns null.
     */
    File file(Object path);

    /**
     * <p>Resolves a file path relative to the project directory of this project and validates it using the given
     * scheme. See {@link PathValidation} for the list of possible validations.</p>
     *
     * @param path An object which toString method value is interpreted as a relative path to the project directory.
     * @param validation The validation to perform on the file.
     * @return The resolved file. Never returns null.
     * @throws InvalidUserDataException When the file does not meet the given validation constraint.
     */
    File file(Object path, PathValidation validation) throws InvalidUserDataException;

    /**
     * <p>Resolves a file path to a URI, relative to the project directory of this project. Evaluates the provided path
     * object as described for {@link #file(Object)}, with the exception that any URI scheme is supported, not just
     * 'file:' URIs.</p>
     *
     * @param path The object to resolve as a URI.
     * @return The resolved URI. Never returns null.
     */
    URI uri(Object path);

    /**
     * <p>Returns the relative path from the project directory to the given path. The given path object is (logically)
     * resolved as described for {@link #file(Object)}, from which a relative path is calculated.</p>
     *
     * @param path The path to convert to a relative path.
     * @return The relative path. Never returns null.
     */
    String relativePath(Object path);

    /**
     * <p>Returns a {@link ConfigurableFileCollection} containing the given files. You can pass any of the following
     * types to this method:</p>
     *
     * <ul> <li>A {@code String}. Interpreted relative to the project directory, as for {@link #file(Object)}. A string
     * that starts with {@code file:} is treated as a file URL.</li>
     *
     * <li>A {@code File}. Interpreted relative to the project directory, as for {@link #file(Object)}.</li>
     *
     * <li>{@link java.net.URI} or {@link java.net.URL}. The URL's path is interpreted as a file path. Currently, only
     * {@code file:} URLs are supported.
     *
     * <li>A {@code Collection} or an array. May contain any of the types listed here. The elements of the collection
     * are recursively converted to files.</li>
     *
     * <li>A {@link org.gradle.api.file.FileCollection}. The contents of the collection are included in the returned
     * collection.</li>
     *
     * <li>A {@link java.util.concurrent.Callable}. The {@code call()} method may return any of the types listed here.
     * The return value of the {@code call()} method is recursively converted to files. A {@code null} return value is
     * treated as an empty collection.</li>
     *
     * <li>A Closure. May return any of the types listed here. The return value of the closure is recursively converted
     * to files. A {@code null} return value is treated as an empty collection.</li>
     *
     * <li>An Object. Its {@code toString()} value is treated the same way as a String, as for {@link
     * #file(Object)}.</li> </ul>
     *
     * <p>The returned file collection is lazy, so that the paths are evaluated only when the contents of the file
     * collection are queried. The file collection is also live, so that it evaluates the above each time the contents
     * of the collection is queried.</p>
     *
     * <p>The returned file collection maintains the iteration order of the supplied paths.</p>
     *
     * @param paths The paths to the files. May be empty.
     * @return The file collection. Never returns null.
     */
    ConfigurableFileCollection files(Object... paths);

    /**
     * <p>Creates a new {@code ConfigurableFileCollection} using the given paths. The paths are evaluated as for {@link
     * #files(Object...)}. The file collection is configured using the given closure. The file collection is passed to
     * the closure as its delegate. Example:</p>
     * <pre>
     * files "$buildDir/classes" {
     *     builtBy 'compile'
     * }
     * </pre>
     * <p>The returned file collection is lazy, so that the paths are evaluated only when the contents of the file
     * collection are queried. The file collection is also live, so that it evaluates the above each time the contents
     * of the collection is queried.</p>
     *
     * @param paths The contents of the file collection. Evaluated as for {@link #files(Object...)}.
     * @param configureClosure The closure to use to configure the file collection.
     * @return the configured file tree. Never returns null.
     */
    ConfigurableFileCollection files(Object paths, Closure configureClosure);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the given base directory. The given baseDir path is evaluated
     * as for {@link #file(Object)}.</p>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param baseDir The base directory of the file tree. Evaluated as for {@link #file(Object)}.
     * @return the file tree. Never returns null.
     */
    ConfigurableFileTree fileTree(Object baseDir);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the provided map of arguments.  The map will be applied as
     * properties on the new file tree.  Example:</p>
     *
     * <pre>
     * fileTree(dir:'src', excludes:['**&#47;ignore/**','**&#47;.svn/**'])
     * </pre>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param args map of property assignments to {@code ConfigurableFileTree} object
     * @return the configured file tree. Never returns null.
     */
    ConfigurableFileTree fileTree(Map<String, ?> args);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the provided closure.  The closure will be used to configure
     * the new file tree. The file tree is passed to the closure as its delegate.  Example:</p>
     *
     * <pre>
     * fileTree {
     *    from 'src'
     *    exclude '**&#47;.svn/**'
     * }.copy { into 'dest'}
     * </pre>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param closure Closure to configure the {@code ConfigurableFileTree} object
     * @return the configured file tree. Never returns null.
     */
    ConfigurableFileTree fileTree(Closure closure);

    /**
     * <p>Creates a new {@code FileTree} which contains the contents of the given ZIP file. The given zipPath path is
     * evaluated as for {@link #file(Object)}. You can combine this method with the {@link #copy(groovy.lang.Closure)}
     * method to unzip a ZIP file.</p>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param zipPath The ZIP file. Evaluated as for {@link #file(Object)}.
     * @return the file tree. Never returns null.
     */
    FileTree zipTree(Object zipPath);

    /**
     * <p>Creates a new {@code FileTree} which contains the contents of the given TAR file. The given tarPath path is
     * evaluated as for {@link #file(Object)}. You can combine this method with the {@link #copy(groovy.lang.Closure)}
     * method to untar a TAR file.</p>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param tarPath The TAR file. Evaluated as for {@link #file(Object)}.
     * @return the file tree. Never returns null.
     */
    FileTree tarTree(Object tarPath);

    /**
     * Creates a directory and returns a file pointing to it.
     *
     * @param path The path for the directory to be created. Evaluated as for {@link #file(Object)}.
     * @return the created directory
     * @throws org.gradle.api.InvalidUserDataException If the path points to an existing file.
     */
    File mkdir(Object path);

    /**
     * Deletes files and directories.
     *
     * @param paths Any type of object accepted by {@link org.gradle.api.Project#files(Object...)}
     * @return true if anything got deleted, false otherwise
     */
    boolean delete(Object... paths);

    /**
     * Executes a Java main class. The closure configures a {@link org.gradle.process.JavaExecSpec}.
     *
     * @param closure The closure for configuring the execution.
     * @return the result of the execution
     */
    ExecResult javaexec(Closure closure);

    /**
     * Executes an external command. The closure configures a {@link org.gradle.process.ExecSpec}.
     *
     * @param closure The closure for configuring the execution.
     * @return the result of the execution
     */
    ExecResult exec(Closure closure);

    /**
     * <p>Converts a name to an absolute project path, resolving names relative to this project.</p>
     *
     * @param path The path to convert.
     * @return The absolute path.
     * @deprecated Use {@link #absoluteProjectPath(String)} instead.
     */
    @Deprecated
    String absolutePath(String path);

    /**
     * <p>Converts a name to an absolute project path, resolving names relative to this project.</p>
     *
     * @param path The path to convert.
     * @return The absolute path.
     */
    String absoluteProjectPath(String path);

    /**
     * <p>Converts a name to a project path relative to this project.</p>
     *
     * @param path The path to convert.
     * @return The relative path.
     */
    String relativeProjectPath(String path);

    /**
     * <p>Returns the <code>AntBuilder</code> for this project. You can use this in your build file to execute ant
     * tasks.</p>
     *
     * @return The <code>AntBuilder</code> for this project. Never returns null.
     */
    AntBuilder getAnt();

    /**
     * <p>Creates an additional <code>AntBuilder</code> for this project. You can use this in your build file to execute
     * ant tasks.</p>
     *
     * @return Creates an <code>AntBuilder</code> for this project. Never returns null.
     * @see #getAnt()
     */
    AntBuilder createAntBuilder();

    /**
     * <p>Executes the given closure against the <code>AntBuilder</code> for this project. You can use this in your
     * build file to execute ant tasks. The <code>AntBuild</code> is passed to the closure as the closure's
     * delegate.</p>
     *
     * @param configureClosure The closure to execute against the <code>AntBuilder</code>.
     * @return The <code>AntBuilder</code>. Never returns null.
     */
    AntBuilder ant(Closure configureClosure);

    /**
     * Returns the configurations of this project.
     *
     * @return The configuration of this project.
     */
    ConfigurationContainer getConfigurations();

    /**
     * <p>Configures the dependency configurations for this project.
     *
     * <p>This method executes the given closure against the {@link ConfigurationContainer} for this project. The {@link
     * ConfigurationContainer} is passed to the closure as the closure's delegate.
     *
     * @param configureClosure the closure to use to configure the dependency configurations.
     */
    void configurations(Closure configureClosure);

    /**
     * Returns a handler for assigning artifacts produced by the project to configurations.
     */
    ArtifactHandler getArtifacts();

    /**
     * <p>Configures the published artifacts for this project.
     *
     * <p>This method executes the given closure against the {@link ArtifactHandler} for this project. The {@link
     * ArtifactHandler} is passed to the closure as the closure's delegate.
     *
     * @param configureClosure the closure to use to configure the published artifacts.
     */
    void artifacts(Closure configureClosure);

    /**
     * <p>Returns the {@link Convention} for this project.</p> <p/> <p>You can access this property in your build file
     * using <code>convention</code>. You can also can also access the properties and methods of the convention object
     * as if they were properties and methods of this project. See <a href="#properties">here</a> for more details</p>
     *
     * @return The <code>Convention</code>. Never returns null.
     */
    Convention getConvention();

    /**
     * <p>Compares the nesting level of this project with another project of the multi-project hierarchy.</p>
     *
     * @param otherProject The project to compare the nesting level with.
     * @return a negative integer, zero, or a positive integer as this project has a nesting level less than, equal to,
     *         or greater than the specified object.
     * @see #getDepth()
     */
    int depthCompare(Project otherProject);

    /**
     * <p>Returns the nesting level of a project in a multi-project hierarchy. For single project builds this is always
     * 0. In a multi-project hierarchy 0 is returned for the root project.</p>
     */
    int getDepth();

    /**
     * <p>Returns the tasks of this project.</p>
     *
     * @return the tasks of this project.
     */
    TaskContainer getTasks();

    /**
     * <p>Configures the sub-projects of this project</p>
     *
     * <p>This method executes the given {@link Action} against the sub-projects of this project.</p>
     *
     * @param action The action to execute.
     */
    void subprojects(Action<? super Project> action);

    /**
     * <p>Configures the sub-projects of this project.</p>
     *
     * <p>This method executes the given closure against each of the sub-projects of this project. The target {@link
     * Project} is passed to the closure as the closure's delegate.</p>
     *
     * @param configureClosure The closure to execute.
     */
    void subprojects(Closure configureClosure);

    /**
     * <p>Configures this project and each of its sub-projects.</p>
     *
     * <p>This method executes the given {@link Action} against this project and each of its sub-projects.</p>
     *
     * @param action The action to execute.
     */
    void allprojects(Action<? super Project> action);

    /**
     * <p>Configures this project and each of its sub-projects.</p>
     *
     * <p>This method executes the given closure against this project and its sub-projects. The target {@link Project}
     * is passed to the closure as the closure's delegate.</p>
     *
     * @param configureClosure The closure to execute.
     */
    void allprojects(Closure configureClosure);

    /**
     * Adds an action to execute immediately before this project is evaluated.
     *
     * @param action the action to execute.
     */
    void beforeEvaluate(Action<? super Project> action);

    /**
     * Adds an action to execute immediately after this project is evaluated.
     *
     * @param action the action to execute.
     */
    void afterEvaluate(Action<? super Project> action);

    /**
     * <p>Adds a closure to be called immediately before this project is evaluated. The project is passed to the closure
     * as a parameter.</p>
     *
     * @param closure The closure to call.
     */
    void beforeEvaluate(Closure closure);

    /**
     * <p>Adds a closure to be called immediately after this project has been evaluated. The project is passed to the
     * closure as a parameter. Such a listener gets notified when the build file belonging to this project has been
     * executed. A parent project may for example add such a listener to its child project. Such a listener can futher
     * configure those child projects based on the state of the child projects after their build files have been
     * run.</p>
     *
     * @param closure The closure to call.
     */
    void afterEvaluate(Closure closure);

    /**
     * <p>Determines if this project has the given property. See <a href="#properties">here</a> for details of the
     * properties which are available for a project.</p>
     *
     * @param propertyName The name of the property to locate.
     * @return True if this project has the given property, false otherwise.
     */
    boolean hasProperty(String propertyName);

    /**
     * <p>Returns the properties of this project. See <a href="#properties">here</a> for details of the properties which
     * are available for a project.</p>
     *
     * @return A map from property name to value.
     */
    Map<String, ?> getProperties();

    /**
     * <p>Returns the value of the given property.  This method locates a property as follows:</p>
     *
     * <ol>
     *
     * <li>If this project object has a property with the given name, return the value of the property.</li>
     *
     * <li>If this project's convention object has a property with the given name, return the value of the
     * property.</li>
     *
     * <li>If this project has an additional property with the given name, return the value of the property.</li>
     *
     * <li>If this project has a task with the given name, return the task.</li>
     *
     * <li>Search up through this project's ancestor projects for a convention property or additional property with the
     * given name.</li>
     *
     * <li>If not found, throw {@link MissingPropertyException}</li>
     *
     * </ol>
     *
     * @param propertyName The name of the property.
     * @return The value of the property, possibly null.
     * @throws MissingPropertyException When the given property is unknown.
     */
    Object property(String propertyName) throws MissingPropertyException;

    /**
     * <p>Returns the logger for this project. You can use this in your build file to write log messages.</p>
     *
     * @return The logger. Never returns null.
     */
    Logger getLogger();

    /**
     * <p>Returns the {@link org.gradle.api.invocation.Gradle} invocation which this project belongs to.</p>
     *
     * @return The Gradle object. Never returns null.
     */
    Gradle getGradle();

    /**
     * Returns the {@link org.gradle.api.logging.LoggingManager} which can be used to control the logging level and
     * standard output/error capture for this project's build script. By default, System.out is redirected to the Gradle
     * logging system at the QUIET log level, and System.err is redirected at the ERROR log level.
     *
     * @return the LoggingManager. Never returns null.
     */
    LoggingManager getLogging();

    /**
     * Disables redirection of standard output during project evaluation. By default redirection is enabled.
     *
     * @see #captureStandardOutput(org.gradle.api.logging.LogLevel)
     */
    @Deprecated
    void disableStandardOutputCapture();

    /**
     * <p>Starts redirection of standard output during to the logging system during project evaluation. By default
     * redirection is enabled and the output is redirected to the QUIET level. System.err is always redirected to the
     * ERROR level. Redirection of output at execution time can be configured via the tasks.</p>
     *
     * <p>In a multi-project this is a per-project setting.</p>
     *
     * @param level The level standard out should be logged to.
     * @see #disableStandardOutputCapture()
     * @see Task#captureStandardOutput(org.gradle.api.logging.LogLevel)
     * @see org.gradle.api.Task#disableStandardOutputCapture()
     * @deprecated Use the {@link org.gradle.api.logging.LoggingManager} returned by {@link #getLogging()} instead
     */
    @Deprecated
    void captureStandardOutput(LogLevel level);

    /**
     * <p>Configures an object via a closure, with the closure's delegate set to the supplied object. This way you don't
     * have to specify the context of a configuration statement multiple times. <p/> Instead of:</p>
     * <pre>
     * MyType myType = new MyType()
     * myType.doThis()
     * myType.doThat()
     * </pre>
     * <p/> you can do:
     * <pre>
     * MyType myType = configure(new MyType()) {
     *     doThis()
     *     doThat()
     * }
     * </pre>
     *
     * <p>The object being configured is also passed to the closure as a parameter, so you can access it explicitly if
     * required:</p>
     * <pre>
     * configure(someObj) { obj -> obj.doThis() }
     * </pre>
     *
     * @param object The object to configure
     * @param configureClosure The closure with configure statements
     * @return The configured object
     */
    Object configure(Object object, Closure configureClosure);

    /**
     * Configures a collection of objects via a closure. This is equivalent to calling {@link #configure(Object,
     * groovy.lang.Closure)} for each of the given objects.
     *
     * @param objects The objects to configure
     * @param configureClosure The closure with configure statements
     * @return The configured objects.
     */
    Iterable<?> configure(Iterable<?> objects, Closure configureClosure);

    /**
     * Configures a collection of objects via an action.
     *
     * @param objects The objects to configure
     * @param configureAction The action to apply to each object
     * @return The configured objects.
     */
    <T> Iterable<T> configure(Iterable<T> objects, Action<? super T> configureAction);

    /**
     * Returns a handler to create repositories which are used for retrieving dependencies and uploading artifacts
     * produced by the project.
     *
     * @return the repository handler. Never returns null.
     */
    RepositoryHandler getRepositories();

    /**
     * <p>Configures the repositories for this project.
     *
     * <p>This method executes the given closure against the {@link RepositoryHandler} for this project. The {@link
     * RepositoryHandler} is passed to the closure as the closure's delegate.
     *
     * @param configureClosure the closure to use to configure the repositories.
     */
    void repositories(Closure configureClosure);

    /**
     * Creates a new repository handler. <p/> Each repository handler is a factory and container for repositories. For
     * example each instance of an upload task has its own repository handler.
     *
     * @return a new repository handler
     */
    RepositoryHandler createRepositoryHandler();

    /**
     * Returns the dependency handler of this project. The returned dependency handler instance can be used for adding
     * new dependencies. For accessing already declared dependencies, the configurations can be used.
     *
     * @return the dependency handler. Never returns null.
     * @see #getConfigurations()
     */
    DependencyHandler getDependencies();

    /**
     * <p>Configures the dependencies for this project.
     *
     * <p>This method executes the given closure against the {@link DependencyHandler} for this project. The {@link
     * DependencyHandler} is passed to the closure as the closure's delegate.
     *
     * @param configureClosure the closure to use to configure the dependencies.
     */
    void dependencies(Closure configureClosure);

    /**
     * Returns the plugins container for this project. The returned container can be used to manage the plugins which
     * are used by this project.
     *
     * @return the plugin container. Never returns null.
     */
    PluginContainer getPlugins();

    /**
     * Returns the build script handler for this project. You can use this handler to query details about the build
     * script for this project, and manage the classpath used to compile and execute the project's build script.
     *
     * @return the classpath handler. Never returns null.
     */
    ScriptHandler getBuildscript();

    /**
     * <p>Configures the build script classpath for this project.
     *
     * <p>The given closure is executed against this project's {@link ScriptHandler}. The {@link ScriptHandler} is
     * passed to the closure as the closure's delegate.
     *
     * @param configureClosure the closure to use to configure the build script classpath.
     */
    void buildscript(Closure configureClosure);

    /**
     * Copies the specified files.  The given closure is used to configure a {@link CopySpec}, which is then used to
     * copy the files. Example:
     * <pre>
     * copy {
     *    from configurations.runtime
     *    into 'build/deploy/lib'
     * }
     * </pre>
     * Note that CopySpecs can be nested:
     * <pre>
     * copy {
     *    into 'build/webroot'
     *    exclude '**&#47;.svn/**'
     *    from('src/main/webapp') {
     *       include '**&#47;*.jsp'
     *       filter(ReplaceTokens, tokens:[copyright:'2009', version:'2.3.1'])
     *    }
     *    from('src/main/js') {
     *       include '**&#47;*.js'
     *    }
     * }
     * </pre>
     *
     * @param closure Closure to configure the CopySpec
     * @return {@link WorkResult} that can be used to check if the copy did any work.
     */
    WorkResult copy(Closure closure);

    /**
     * Creates a {@link CopySpec} which can later be used to copy files or create an archive. The given closure is used
     * to configure the {@link CopySpec} before it is returned by this method.
     *
     * @param closure Closure to configure the CopySpec
     * @return The CopySpec
     */
    CopySpec copySpec(Closure closure);

    /**
     * <p>Configures this project using plugins or scripts. The given closure is used to configure an {@link
     * org.gradle.api.plugins.ObjectConfigurationAction} which is then used to configure this project.</p>
     *
     * @param closure The closure to configure the {@code ObjectConfigurationAction}.
     */
    void apply(Closure closure);

    /**
     * <p>Configures this project using plugins or scripts. The following options are available:</p>
     *
     * <ul><li>{@code from}: A script to apply to the project. Accepts any path supported by {@link #uri(Object)}.</li>
     *
     * <li>{@code plugin}: The id or implementation class of the plugin to apply to the project.</li>
     *
     * <li>{@code to}: The target delegate object or objects. Use this to configure objects other than the
     * project.</li></ul>
     *
     * <p>For more detail, see {@link org.gradle.api.plugins.ObjectConfigurationAction}.</p>
     *
     * @param options The options to use to configure the {@code ObjectConfigurationAction}.
     */
    void apply(Map<String, ?> options);

    /**
     * Returns the evaluation state of this project. You can use this to access information about the evaluation of this
     * project, such as whether it has failed.
     *
     * @return the project state. Never returns null.
     */
    ProjectState getState();

    /**
     * Creates a container for managing named objects of the specified type. The specified type must have a public constructor which takes the name as a String parameter.
     *
     * @param type The type of objects for the container to contain.
     * @param <T> The type of objects for the container to contain.
     * @return The container.
     */
    <T> NamedDomainObjectContainer<T> container(Class<T> type);

    /**
     * Creates a container for managing named objects of the specified type. The given factory is used to create object instances.
     *
     * @param type The type of objects for the container to contain.
     * @param factory The factory to use to create object instances.
     * @param <T> The type of objects for the container to contain.
     * @return The container.
     */
    <T> NamedDomainObjectContainer<T> container(Class<T> type, NamedDomainObjectFactory<? extends T> factory);

    /**
     * Creates a container for managing named objects of the specified type. The given closure is used to create object instances. The name of the instance to be created is passed as a parameter to
     * the closure.
     *
     * @param type The type of objects for the container to contain.
     * @param factoryClosure The closure to use to create object instances.
     * @param <T> The type of objects for the container to contain.
     * @return The container.
     */
    <T> NamedDomainObjectContainer<T> container(Class<T> type, Closure factoryClosure);
}