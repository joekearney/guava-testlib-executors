package libjoe.testlib.executors;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.Executor;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.TesterAnnotation;

//Enum values use constructors with generic varargs.
@SuppressWarnings("unchecked")
@GwtCompatible
public enum ExecutorFeature implements Feature<Executor> {
	EXECUTOR,
	EXECUTOR_SERVICE,
	LISTENABLE,
	SCHEDULED,
	C;

	private final Set<Feature<? super Executor>> implied;

	ExecutorFeature(Feature<? super Executor>... implied) {
		this.implied = Helpers.copyToSet(implied);
	}

	@Override
	public Set<Feature<? super Executor>> getImpliedFeatures() {
		return implied;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@TesterAnnotation
	public @interface Require {
		public abstract ExecutorFeature[] value() default {};
		public abstract ExecutorFeature[] absent() default {};
	}
}