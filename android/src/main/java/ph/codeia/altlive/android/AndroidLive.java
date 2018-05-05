package ph.codeia.altlive.android;

/*
 * This file is a part of the AltLiveData project.
 */

import ph.codeia.altlive.Function;
import ph.codeia.altlive.LiveField;
import ph.codeia.altlive.LiveLoader;
import ph.codeia.altlive.LiveTask;
import ph.codeia.altlive.Try;

public final class AndroidLive {

    public static LiveField.Builder builder() {
        return builder(false);
    }

    public static LiveField.Builder builder(boolean alwaysPost) {
        return new LiveField.Builder().postOn(alwaysPost ?
                Threading.UI_POST_ALWAYS :
                Threading.UI_POST_AS_NEEDED
        );
    }

    public static <T> LiveField<T> field() {
        return builder().build();
    }

    public static <T> LiveField<T> field(LiveField.Builder builder) {
        return builder.postOn(Threading.UI_POST_AS_NEEDED).build();
    }

    public static <T> LiveLoader<T> loader() {
        return loader(builder());
    }

    public static <T> LiveLoader<T> loader(LiveField.Builder builder) {
        return new LiveLoader<>(field(builder));
    }

    public static <I, O> LiveTask<I, O> task(Function<I, Try<? extends O>> block) {
        return task(builder(), block);
    }

    public static <I, O> LiveTask<I, O> task(
            LiveField.Builder builder,
            Function<I, Try<? extends O>> block
    ) {
        return new LiveTask<>(field(builder), block);
    }

    private AndroidLive() {
    }
}

