package ph.codeia.altlive.transform;

/*
 * This file is a part of the AltLiveData project.
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ph.codeia.altlive.Function;
import ph.codeia.altlive.Try;

/**
 * Marker for {@link Try} transformer factories.
 *
 * <p> The result of methods annotated with this can be used as an argument to
 * {@link Try#pipe(Function)}. If the target is a constructor
 * (e.g. {@link Memoize}), it should be passed as a method reference like so:
 * {@code computation.pipe(Memoize::new)}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface Transformer {
}
