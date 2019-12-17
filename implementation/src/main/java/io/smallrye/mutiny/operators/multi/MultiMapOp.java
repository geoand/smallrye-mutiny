package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;

public final class MultiMapOp<T, U> extends AbstractMultiOperator<T, U> {
    private final Function<? super T, ? extends U> mapper;

    public MultiMapOp(Multi<T> upstream, Function<? super T, ? extends U> mapper) {
        super(upstream);
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
    }

    @Override
    public void subscribe(Subscriber<? super U> downstream) {
        if (downstream == null) {
            throw new NullPointerException("Subscriber is `null`");
        }
        upstream.subscribe().withSubscriber(new MapProcessor<T, U>(downstream, mapper));
    }

    @Override
    protected Publisher<U> publisher() {
        return this;
    }

    static class MapProcessor<I, O> extends MultiOperatorProcessor<I, O> {
        private final Function<? super I, ? extends O> mapper;

        MapProcessor(Subscriber<? super O> actual, Function<? super I, ? extends O> mapper) {
            super(actual);
            this.mapper = mapper;
        }

        @Override
        public void onNext(I item) {
            if (isDone()) {
                return;
            }
            O v;
            try {
                v = mapper.apply(item);
            } catch (Throwable ex) {
                failAndCancel(ex);
                return;
            }
            if (v == null) {
                failAndCancel(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                downstream.onNext(v);
            }
        }
    }

}
