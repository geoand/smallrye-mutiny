package io.smallrye.mutiny.operators;

import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.builders.CollectionBasedMulti;

public class MultiCombine {

    private MultiCombine() {
        // avoid direct instantiation.
    }

    public static <T> Multi<T> merge(List<Publisher<T>> participants, boolean collectFailures, int requests,
            int concurrency) {
        List<Publisher<T>> candidates = ParameterValidation.doesNotContainNull(participants, "participants");
        if (collectFailures) {
            return Infrastructure.onMultiCreation(new CollectionBasedMulti<>(candidates)
                    .onItem().produceMulti(Function.identity()).collectFailures().withRequests(requests)
                    .merge(concurrency));
        } else {
            return Infrastructure.onMultiCreation(new CollectionBasedMulti<>(candidates)
                    .onItem().produceMulti(Function.identity()).withRequests(requests).merge(concurrency));
        }
    }
}
