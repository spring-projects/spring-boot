/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.architecture;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.GivenMethodsConjunction;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.util.ResourceUtils;

/**
 * Factory used to create {@link ArchRule architecture rules}.
 *
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Phillip Webb
 */
final class ArchitectureRules {

	private ArchitectureRules() {
	}

	static List<ArchRule> noClassesShouldCallObjectsRequireNonNull() {
		return List.of(
				noClassesShould().callMethod(Objects.class, "requireNonNull", Object.class, String.class)
					.because(shouldUse("org.springframework.utils.Assert.notNull(Object, String)")),
				noClassesShould().callMethod(Objects.class, "requireNonNull", Object.class, Supplier.class)
					.because(shouldUse("org.springframework.utils.Assert.notNull(Object, Supplier)")));
	}

	static List<ArchRule> standard() {
		List<ArchRule> rules = new ArrayList<>();
		rules.add(allPackagesShouldBeFreeOfTangles());
		rules.add(allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization());
		rules.add(allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveOnlyInjectEnvironment());
		rules.add(noClassesShouldCallStepVerifierStepVerifyComplete());
		rules.add(noClassesShouldConfigureDefaultStepVerifierTimeout());
		rules.add(noClassesShouldCallCollectorsToList());
		rules.add(noClassesShouldCallURLEncoderWithStringEncoding());
		rules.add(noClassesShouldCallURLDecoderWithStringEncoding());
		rules.add(noClassesShouldLoadResourcesUsingResourceUtils());
		rules.add(noClassesShouldCallStringToUpperCaseWithoutLocale());
		rules.add(noClassesShouldCallStringToLowerCaseWithoutLocale());
		rules.add(conditionalOnMissingBeanShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodReturnType());
		rules.add(enumSourceShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodParameterType());
		rules.add(classLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute());
		rules.add(methodLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute());
		return List.copyOf(rules);
	}

	private static ArchRule allPackagesShouldBeFreeOfTangles() {
		return SlicesRuleDefinition.slices().matching("(**)").should().beFreeOfCycles();
	}

	private static ArchRule allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization() {
		return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean").and()
			.haveRawReturnType(assignableTo("org.springframework.beans.factory.config.BeanPostProcessor"))
			.should(onlyHaveParametersThatWillNotCauseEagerInitialization())
			.andShould()
			.beStatic()
			.allowEmptyShould(true);
	}

	private static ArchCondition<JavaMethod> onlyHaveParametersThatWillNotCauseEagerInitialization() {
		return check("not have parameters that will cause eager initialization",
				ArchitectureRules::allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization);
	}

	private static void allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization(JavaMethod item,
			ConditionEvents events) {
		DescribedPredicate<JavaParameter> notAnnotatedWithLazy = DescribedPredicate
			.not(CanBeAnnotated.Predicates.annotatedWith("org.springframework.context.annotation.Lazy"));
		DescribedPredicate<JavaClass> notOfASafeType = notAssignableTo(
				"org.springframework.beans.factory.ObjectProvider", "org.springframework.context.ApplicationContext",
				"org.springframework.core.env.Environment")
			.and(notAnnotatedWithRoleInfrastructure());
		item.getParameters()
			.stream()
			.filter(notAnnotatedWithLazy)
			.filter((parameter) -> notOfASafeType.test(parameter.getRawType()))
			.forEach((parameter) -> addViolation(events, parameter,
					parameter.getDescription() + " will cause eager initialization as it is "
							+ notAnnotatedWithLazy.getDescription() + " and is " + notOfASafeType.getDescription()));
	}

	private static DescribedPredicate<JavaClass> notAnnotatedWithRoleInfrastructure() {
		return is("not annotated with @Role(BeanDefinition.ROLE_INFRASTRUCTURE", (candidate) -> {
			Role role = candidate.getAnnotationOfType(Role.class);
			return (role == null) || (role.value() != BeanDefinition.ROLE_INFRASTRUCTURE);
		});
	}

	private static ArchRule allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveOnlyInjectEnvironment() {
		return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean").and()
			.haveRawReturnType(assignableTo("org.springframework.beans.factory.config.BeanFactoryPostProcessor"))
			.should(onlyInjectEnvironment())
			.andShould()
			.beStatic()
			.allowEmptyShould(true);
	}

	private static ArchCondition<JavaMethod> onlyInjectEnvironment() {
		return check("only inject Environment", ArchitectureRules::onlyInjectEnvironment);
	}

	private static void onlyInjectEnvironment(JavaMethod item, ConditionEvents events) {
		if (item.getParameters().stream().anyMatch(ArchitectureRules::isNotEnvironment)) {
			addViolation(events, item, item.getDescription() + " should only inject Environment");
		}
	}

	private static boolean isNotEnvironment(JavaParameter parameter) {
		return !"org.springframework.core.env.Environment".equals(parameter.getType().getName());
	}

	private static ArchRule noClassesShouldCallStepVerifierStepVerifyComplete() {
		return noClassesShould().callMethod("reactor.test.StepVerifier$Step", "verifyComplete")
			.because("it can block indefinitely and " + shouldUse("expectComplete().verify(Duration)"));
	}

	private static ArchRule noClassesShouldConfigureDefaultStepVerifierTimeout() {
		return noClassesShould().callMethod("reactor.test.StepVerifier", "setDefaultTimeout", "java.time.Duration")
			.because(shouldUse("expectComplete().verify(Duration)"));
	}

	private static ArchRule noClassesShouldCallCollectorsToList() {
		return noClassesShould().callMethod(Collectors.class, "toList")
			.because(shouldUse("java.util.stream.Stream.toList()"));
	}

	private static ArchRule noClassesShouldCallURLEncoderWithStringEncoding() {
		return noClassesShould().callMethod(URLEncoder.class, "encode", String.class, String.class)
			.because(shouldUse("java.net.URLEncoder.encode(String s, Charset charset)"));
	}

	private static ArchRule noClassesShouldCallURLDecoderWithStringEncoding() {
		return noClassesShould().callMethod(URLDecoder.class, "decode", String.class, String.class)
			.because(shouldUse("java.net.URLDecoder.decode(String s, Charset charset)"));
	}

	private static ArchRule noClassesShouldLoadResourcesUsingResourceUtils() {
		DescribedPredicate<JavaCall<?>> resourceUtilsGetURL = hasJavaCallTarget(ownedByResourceUtils())
			.and(hasJavaCallTarget(hasNameOf("getURL")))
			.and(hasJavaCallTarget(hasRawStringParameterType()));
		DescribedPredicate<JavaCall<?>> resourceUtilsGetFile = hasJavaCallTarget(ownedByResourceUtils())
			.and(hasJavaCallTarget(hasNameOf("getFile")))
			.and(hasJavaCallTarget(hasRawStringParameterType()));
		return noClassesShould().callMethodWhere(resourceUtilsGetURL.or(resourceUtilsGetFile))
			.because(shouldUse("org.springframework.boot.io.ApplicationResourceLoader"));
	}

	private static ArchRule noClassesShouldCallStringToUpperCaseWithoutLocale() {
		return noClassesShould().callMethod(String.class, "toUpperCase")
			.because(shouldUse("String.toUpperCase(Locale.ROOT)"));
	}

	private static ArchRule noClassesShouldCallStringToLowerCaseWithoutLocale() {
		return noClassesShould().callMethod(String.class, "toLowerCase")
			.because(shouldUse("String.toLowerCase(Locale.ROOT)"));
	}

	private static ArchRule conditionalOnMissingBeanShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodReturnType() {
		return methodsThatAreAnnotatedWith("org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean")
			.should(notSpecifyOnlyATypeThatIsTheSameAsTheMethodReturnType())
			.allowEmptyShould(true);
	}

	private static ArchCondition<? super JavaMethod> notSpecifyOnlyATypeThatIsTheSameAsTheMethodReturnType() {
		return check("not specify only a type that is the same as the method's return type", (item, events) -> {
			JavaAnnotation<JavaMethod> conditionalAnnotation = item
				.getAnnotationOfType("org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean");
			Map<String, Object> properties = conditionalAnnotation.getProperties();
			if (!properties.containsKey("type") && !properties.containsKey("name")) {
				conditionalAnnotation.get("value").ifPresent((value) -> {
					if (containsOnlySingleType((JavaType[]) value, item.getReturnType())) {
						addViolation(events, item, conditionalAnnotation.getDescription()
								+ " should not specify only a value that is the same as the method's return type");
					}
				});
			}
		});
	}

	private static ArchRule enumSourceShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodParameterType() {
		return ArchRuleDefinition.methods()
			.that()
			.areAnnotatedWith("org.junit.jupiter.params.provider.EnumSource")
			.should(notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType())
			.allowEmptyShould(true);
	}

	private static ArchCondition<? super JavaMethod> notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType() {
		return check("not specify only a type that is the same as the method's parameter type",
				ArchitectureRules::notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType);
	}

	private static void notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType(JavaMethod item,
			ConditionEvents events) {
		JavaAnnotation<JavaMethod> enumSourceAnnotation = item
			.getAnnotationOfType("org.junit.jupiter.params.provider.EnumSource");
		Map<String, Object> properties = enumSourceAnnotation.getProperties();
		if (properties.size() == 1 && item.getParameterTypes().size() == 1) {
			enumSourceAnnotation.get("value").ifPresent((value) -> {
				if (value.equals(item.getParameterTypes().get(0))) {
					addViolation(events, item, enumSourceAnnotation.getDescription()
							+ " should not specify only a value that is the same as the method's parameter type");
				}
			});
		}
	}

	private static ArchRule classLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute() {
		return ArchRuleDefinition.classes()
			.that()
			.areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
			.should(notSpecifyOnlyPrefixAttributeOfConfigurationProperties())
			.allowEmptyShould(true);
	}

	private static ArchRule methodLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute() {
		return ArchRuleDefinition.methods()
			.that()
			.areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
			.should(notSpecifyOnlyPrefixAttributeOfConfigurationProperties())
			.allowEmptyShould(true);
	}

	private static ArchCondition<? super HasAnnotations<?>> notSpecifyOnlyPrefixAttributeOfConfigurationProperties() {
		return check("not specify only prefix attribute of @ConfigurationProperties",
				ArchitectureRules::notSpecifyOnlyPrefixAttributeOfConfigurationProperties);
	}

	private static void notSpecifyOnlyPrefixAttributeOfConfigurationProperties(HasAnnotations<?> item,
			ConditionEvents events) {
		JavaAnnotation<?> configurationPropertiesAnnotation = item
			.getAnnotationOfType("org.springframework.boot.context.properties.ConfigurationProperties");
		Map<String, Object> properties = configurationPropertiesAnnotation.getProperties();
		if (properties.size() == 1 && properties.containsKey("prefix")) {
			addViolation(events, item, configurationPropertiesAnnotation.getDescription()
					+ " should specify implicit 'value' attribute other than explicit 'prefix' attribute");
		}
	}

	private static boolean containsOnlySingleType(JavaType[] types, JavaType type) {
		return types.length == 1 && type.equals(types[0]);
	}

	private static ClassesShould noClassesShould() {
		return ArchRuleDefinition.noClasses().should();
	}

	private static GivenMethodsConjunction methodsThatAreAnnotatedWith(String annotation) {
		return ArchRuleDefinition.methods().that().areAnnotatedWith(annotation);
	}

	private static DescribedPredicate<HasOwner<JavaClass>> ownedByResourceUtils() {
		return With.owner(Predicates.type(ResourceUtils.class));
	}

	private static DescribedPredicate<? super CodeUnitCallTarget> hasNameOf(String name) {
		return HasName.Predicates.name(name);
	}

	private static DescribedPredicate<HasParameterTypes> hasRawStringParameterType() {
		return HasParameterTypes.Predicates.rawParameterTypes(String.class);
	}

	private static DescribedPredicate<JavaCall<?>> hasJavaCallTarget(
			DescribedPredicate<? super CodeUnitCallTarget> predicate) {
		return JavaCall.Predicates.target(predicate);
	}

	private static DescribedPredicate<JavaClass> notAssignableTo(String... typeNames) {
		return DescribedPredicate.not(assignableTo(typeNames));
	}

	private static DescribedPredicate<JavaClass> assignableTo(String... typeNames) {
		DescribedPredicate<JavaClass> result = null;
		for (String typeName : typeNames) {
			DescribedPredicate<JavaClass> assignableTo = Predicates.assignableTo(typeName);
			result = (result != null) ? result.or(assignableTo) : assignableTo;
		}
		return result;
	}

	private static DescribedPredicate<JavaClass> is(String description, Predicate<JavaClass> predicate) {
		return new DescribedPredicate<>(description) {

			@Override
			public boolean test(JavaClass t) {
				return predicate.test(t);
			}

		};
	}

	private static <T> ArchCondition<T> check(String description, BiConsumer<T, ConditionEvents> check) {
		return new ArchCondition<>(description) {

			@Override
			public void check(T item, ConditionEvents events) {
				check.accept(item, events);
			}

		};
	}

	private static void addViolation(ConditionEvents events, Object correspondingObject, String message) {
		events.add(SimpleConditionEvent.violated(correspondingObject, message));
	}

	private static String shouldUse(String string) {
		return string + " should be used instead";
	}

}
