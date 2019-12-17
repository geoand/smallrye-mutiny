package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniCreateFromPublisher<O> extends UniOperator<Void, O> {
    private final Publisher<? extends O> publisher;

    public UniCreateFromPublisher(Publisher<? extends O> publisher) {
        super(null);
        this.publisher = nonNull(publisher, "publisher");
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Subscriber<O> actual = new Subscriber<O>() {
            @Override
            public void onSubscribe(Subscription s) {
                if (reference.compareAndSet(null, s)) {
                    subscriber.onSubscribe(() -> {
                        Subscription old = reference.getAndSet(CANCELLED);
                        if (old != null) {
                            old.cancel();
                        }
                    });
                    s.request(1);
                } else {
                    s.cancel();
                }
            }

            @Override
            public void onNext(O o) {
                Subscription sub = reference.getAndSet(CANCELLED);
                if (sub == CANCELLED) {
                    // Already cancelled, do nothing
                    return;
                }
                sub.cancel();
                subscriber.onItem(o);
            }

            @Override
            public void onError(Throwable t) {
                subscriber.onFailure(t);
            }

            @Override
            public void onComplete() {
                subscriber.onItem(null);
            }
        };
        Subscriber<? super O> sub = Infrastructure.onMultiSubscription(publisher, actual);
        publisher.subscribe(sub);
    }
}
