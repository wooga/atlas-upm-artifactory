package wooga.gradle.upm.artifactory.internal

import org.gradle.api.Describable
import org.gradle.api.Task
import org.gradle.api.internal.provider.AbstractProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.internal.Describables
import org.gradle.internal.state.ModelObject

import java.lang.reflect.Method

class Extensions {

    static <T> void setPropertiesOwner(Class<? extends T> extensionClass, T owner, String ownerName) {
        Arrays.stream(extensionClass.methods)
                .filter { Property.isAssignableFrom(it.returnType) }
                .filter { it.parameterCount == 0 }
                .filter { [Input, InputDirectory, InputFile, Internal].any { annotation -> it.isAnnotationPresent(annotation) } }
                .map { PropertyInfo.fromGetter(owner, it) }
                .forEach { it.attachPropertyOwner(ownerName) }
    }
}

class ExtensionModelObject implements ModelObject {

    final String extensionName

    ExtensionModelObject(String extensionName) {
        this.extensionName = extensionName
    }

    @Override
    Describable getModelIdentityDisplayName() {
        return Describables.of(extensionName)
    }

    @Override
    boolean hasUsefulDisplayName() {
        return false
    }

    @Override
    Task getTaskThatOwnsThisObject() {
        return null
    }
}

class PropertyInfo {

    boolean isOptional
    String name
    Property property

    static PropertyInfo fromGetter(Object owner, Method getter) {
        return new PropertyInfo().with {
            isOptional = getter.isAnnotationPresent(Optional)
            name = getter.name.replace("get", "").uncapitalize()
            property = getter.invoke(owner) as Property<?>
            return it
        }
    }

    void attachPropertyOwner(String ownerName) {
        if (property instanceof AbstractProperty) {
            def absProperty = property as AbstractProperty
            absProperty.attachOwner(new ExtensionModelObject(ownerName), Describables.of(name))
        }
    }
}
