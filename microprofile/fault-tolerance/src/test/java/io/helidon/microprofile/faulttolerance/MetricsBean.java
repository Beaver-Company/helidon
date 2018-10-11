/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.microprofile.faulttolerance;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Class MetricsBean.
 */
@Dependent
public class MetricsBean {

    private AtomicInteger invocations = new AtomicInteger(0);

    @Inject
    @Metric(name = "counter")
    private Counter counter;

    public Counter getCounter() {
        return counter;
    }

    @Retry(maxRetries = 5, delay = 50L)
    public String retryOne(int number) {
        if (invocations.incrementAndGet() <= number) {
            FaultToleranceTest.printStatus("MetricsBean::retryOne()", "failure");
            throw new RuntimeException("Oops");
        }
        FaultToleranceTest.printStatus("MetricsBean::retryOne()", "success");
        return "success";
    }

    @Retry(maxRetries = 5, delay = 50L)
    public String retryTwo(int number) {
        if (invocations.incrementAndGet() <= number) {
            FaultToleranceTest.printStatus("MetricsBean::retryTwo()", "failure");
            throw new RuntimeException("Oops");
        }
        FaultToleranceTest.printStatus("MetricsBean::retryTwo()", "success");
        return "success";
    }

    @Retry(maxRetries = 5, delay = 50L)
    public String retryThree(int number) {
        if (invocations.incrementAndGet() <= number) {
            FaultToleranceTest.printStatus("MetricsBean::retryThree()", "failure");
            throw new RuntimeException("Oops");
        }
        FaultToleranceTest.printStatus("MetricsBean::retryThree()", "success");
        return "success";
    }

    @Retry(maxRetries = 5, delay = 50L)
    public String retryFour(int number) {
        if (invocations.incrementAndGet() <= number) {
            FaultToleranceTest.printStatus("MetricsBean::retryFour()", "failure");
            throw new RuntimeException("Oops");
        }
        FaultToleranceTest.printStatus("MetricsBean::retryFour()", "success");
        return "success";
    }

    @Retry(maxRetries = 5, delay = 50L)
    public String retryFive(int number) {
        FaultToleranceTest.printStatus("MetricsBean::retryFive()", "success");
        return "success";
    }

    @Timeout(value = 1000, unit = ChronoUnit.MILLIS)
    public String forceTimeout() throws InterruptedException {
        FaultToleranceTest.printStatus("MetricsBean::forceTimeout()", "failure");
        Thread.sleep(1500);
        return "failure";
    }

    @Timeout(value = 1000, unit = ChronoUnit.MILLIS)
    public String noTimeout() throws InterruptedException {
        FaultToleranceTest.printStatus("MetricsBean::noTimeout()", "success");
        Thread.sleep(500);
        return "success";
    }

    @CircuitBreaker(
        successThreshold = CircuitBreakerBean.SUCCESS_THRESHOLD,
        requestVolumeThreshold = CircuitBreakerBean.REQUEST_VOLUME_THRESHOLD,
        failureRatio = CircuitBreakerBean.FAILURE_RATIO,
        delay = CircuitBreakerBean.DELAY)
    public void exerciseBreaker(boolean success) {
        if (success) {
            FaultToleranceTest.printStatus("MetricsBean::exerciseBreaker()", "success");
        } else {
            FaultToleranceTest.printStatus("MetricsBean::exerciseBreaker()", "failure");
            throw new RuntimeException("Oops");
        }
    }

    @CircuitBreaker(
        successThreshold = CircuitBreakerBean.SUCCESS_THRESHOLD,
        requestVolumeThreshold = CircuitBreakerBean.REQUEST_VOLUME_THRESHOLD,
        failureRatio = CircuitBreakerBean.FAILURE_RATIO,
        delay = CircuitBreakerBean.DELAY)
    public void exerciseGauges(boolean success) {
        if (success) {
            FaultToleranceTest.printStatus("MetricsBean::exerciseGauges()", "success");
        } else {
            FaultToleranceTest.printStatus("MetricsBean::exerciseGauges()", "failure");
            throw new RuntimeException("Oops");
        }
    }

    @Fallback(fallbackMethod = "onFailure")
    public String fallback() {
        FaultToleranceTest.printStatus("MetricsBean::fallback()", "failure");
        throw new RuntimeException("Oops");
    }

    public String onFailure() {
        FaultToleranceTest.printStatus("MetricsBean::onFailure()", "success");
        return "fallback";
    }

    @Bulkhead(value = 3, waitingTaskQueue = 3)
    public String concurrent(long sleepMillis) {
        FaultToleranceTest.printStatus("MetricsBean::concurrent()", "success");
        try {
            assertThat(FaultToleranceMetrics.getGauge(this, "concurrent",
                                                                    FaultToleranceMetrics.BULKHEAD_CONCURRENT_EXECUTIONS, long.class).getValue(),
                                     is(not(0)));
            Thread.sleep(sleepMillis);
        } catch (Exception e) {
            // falls through
        }
        return "success";
    }

    @Asynchronous
    @Bulkhead(value = 3, waitingTaskQueue = 3)
    public Future<String> concurrentAsync(long sleepMillis) {
        FaultToleranceTest.printStatus("MetricsBean::concurrentAsync()", "success");
        try {
            assertThat((long) FaultToleranceMetrics.getGauge(this, "concurrentAsync",
                                                             FaultToleranceMetrics.BULKHEAD_WAITING_QUEUE_POPULATION, long.class).getValue(),
                       is(greaterThanOrEqualTo(0L)));
            Thread.sleep(sleepMillis);
        } catch (Exception e) {
            // falls through
        }
        return CompletableFuture.completedFuture("success");
    }
}