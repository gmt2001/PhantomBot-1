/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.util.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides an interface to a shared {@link ScheduledExecutorService}
 *
 * @author gmt2001
 */
public final class ExecutorService {
    /**
     * Shared {@link ScheduledExecutorService} implementing a thread pool
     */
    private static final ScheduledExecutorService SCHEDULEDEXECUTOR = Executors.newScheduledThreadPool(4);
    /**
     * Shared {@link java.util.concurrent.ExecutorService} implementing virtual threads
     */
    private static final java.util.concurrent.ExecutorService VIRTUALEXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    /**
     * Indicates if the executors are shutdown
     */
    private static boolean shutdown = false;

    private ExecutorService() {
    }

    /**
     * A {@link ScheduledExecutorService} implementing a thread pool
     *
     * @return the {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService executorService() {
        return SCHEDULEDEXECUTOR;
    }

    /**
     * An {@link java.util.concurrent.ExecutorService} that provides virtual threads
     *
     * @return the virtual threads {@link java.util.concurrent.ExecutorService}
     */
    public static java.util.concurrent.ExecutorService virtualExecutorService() {
        return VIRTUALEXECUTOR;
    }

    /**
     * Creates and executes a {@link ScheduledFuture} that becomes enabled after the given delay
     *
     * @param <V> the return type of the callable
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return a {@link ScheduledFuture} that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if callable is {@code null}
     */
    public static <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.schedule(callable, delay, unit);
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return a {@link ScheduledFuture} representing pending completion of the task and whose {@link ScheduledFuture#get()} method will return {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is {@code null}
     */
    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.schedule(command, delay, unit);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given period; that
     * is executions will commence after {@code initialDelay} then {@code initialDelay + period}, then {@code initialDelay + 2 * period}, and so on. If
     * any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via
     * cancellation or termination of the executor. If any execution of this task takes longer than its period, then subsequent executions may start
     * late, but will not concurrently execute
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@link ScheduledFuture#get()} method will throw an exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is {@code null}
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given delay between
     * the termination of one execution and the commencement of the next. If any execution of the task encounters an exception, subsequent executions
     * are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return a {@link ScheduledFuture} representing pending completion of the task, and whose {@link ScheduledFuture#get()} method will throw an exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if command is {@code null}
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /**
     * Executes the given command at some time in the future. The command may execute in a new thread, in a pooled thread, or in the calling thread,
     * at the discretion of the {@code Executor} implementation
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be accepted for execution
     * @throws NullPointerException if command is {@code null}
     */
    public static void execute(Runnable command) {
        if (shutdown) {
            return;
        }

        SCHEDULEDEXECUTOR.execute(command);
    }

    /**
     * Executes the given tasks, returning a list of {@link Future} holding their status and results when all complete.
     * {@link Future#isDone()} is {@code true} for each element of the returned list. Note that a <i>completed</i> task could have
     * terminated either normally or by throwing an exception. The results of this method are undefined if the given collection is
     * modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @return a list of {@link Future} representing the tasks, in the same sequential order as produced by the iterator for
     * the given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (shutdown) {
            return Collections.emptyList();
        }

        return SCHEDULEDEXECUTOR.invokeAll(tasks);
    }

    /**
     * Executes the given tasks, returning a list of {@link Future} holding their status and results when all complete or the timeout
     * expires, whichever happens first. {@link Future#isDone()} is {@code true} for each element of the returned list. Note
     * that a <i>completed</i> task could have terminated either normally or by throwing an exception. The results of this
     * method are undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return a list of {@link Future} representing the tasks, in the same sequential order as produced by the iterator for
     * the given task list. If the operation did not time out, each task will have completed. If it did time out, some of
     * these tasks will not have completed
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (shutdown) {
            return Collections.emptyList();
        }

        return SCHEDULEDEXECUTOR.invokeAll(tasks, timeout, unit);
    }

    /**
     * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception),
     * if any do. Upon normal or exceptional return, tasks that have not completed are cancelled. The results of this method are
     * undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task subject to execution is {@code null}
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.invokeAny(tasks);
    }

    /**
     * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception),
     * if any do before the given timeout elapses. Upon normal or exceptional return, tasks that have not completed are cancelled.
     * The results of this method are undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.invokeAny(tasks, timeout, unit);
    }

    /**
     * Submits a Runnable task for execution and returns a {@link Future} representing that task, whose {@link Future#get()} method will return {@code null}
     * upon successful completion
     *
     * @param task the task to submit
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static Future<?> submit(Runnable task) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.submit(task);
    }

    /**
     * Submits a Runnable task for execution and returns a {@link Future} representing that task, whose {@link Future#get()} method will return the given result
     * upon successful completion
     *
     * @param <T> the return type of result
     * @param task the task to submit
     * @param result the result to return
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static <T> Future<T> submit(Runnable task, T result) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.submit(task, result);
    }

    /**
     * Submits a value-returning task for execution and returns a {@link Future} representing the pending results of the task, whose {@link Future#get()} method
     * will return the task's result upon successful completion
     * <p>
     * If you would like to immediately block waiting for a task, you can use constructions of the form {@code result = exec.submit(aCallable).get();}
     * <p>
     * Note: The {@link Executors} class includes a set of methods that can convert some other common closure-like objects, for example,
     * {@link java.security.PrivilegedAction} to {@link Callable} form so they can be submitted
     *
     * @param <T> the return type of the callable
     * @param task the task to submit
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static <T> Future<T> submit(Callable<T> task) {
        if (shutdown) {
            return null;
        }

        return SCHEDULEDEXECUTOR.submit(task);
    }

    /**
     * Executes the given command on a virtual thread at some time in the future. The command may execute in a new thread, in a pooled thread, or in the calling thread,
     * at the discretion of the {@code Executor} implementation
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be accepted for execution
     * @throws NullPointerException if command is {@code null}
     */
    public static void executeVirtual(Runnable command) {
        if (shutdown) {
            return;
        }

        VIRTUALEXECUTOR.execute(command);
    }

    /**
     * Executes the given tasks on virtual threads, returning a list of {@link Future} holding their status and results when all complete.
     * {@link Future#isDone()} is {@code true} for each element of the returned list. Note that a <i>completed</i> task could have
     * terminated either normally or by throwing an exception. The results of this method are undefined if the given collection is
     * modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @return a list of {@link Future} representing the tasks, in the same sequential order as produced by the iterator for
     * the given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> List<Future<T>> invokeAllVirtual(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (shutdown) {
            return Collections.emptyList();
        }

        return VIRTUALEXECUTOR.invokeAll(tasks);
    }

    /**
     * Executes the given tasks on virtual threads, returning a list of {@link Future} holding their status and results when all complete or the timeout
     * expires, whichever happens first. {@link Future#isDone()} is {@code true} for each element of the returned list. Note
     * that a <i>completed</i> task could have terminated either normally or by throwing an exception. The results of this
     * method are undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return a list of {@link Future} representing the tasks, in the same sequential order as produced by the iterator for
     * the given task list. If the operation did not time out, each task will have completed. If it did time out, some of
     * these tasks will not have completed
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> List<Future<T>> invokeAllVirtual(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (shutdown) {
            return Collections.emptyList();
        }

        return VIRTUALEXECUTOR.invokeAll(tasks, timeout, unit);
    }

    /**
     * Executes the given tasks on virtual threads, returning the result of one that has completed successfully (i.e., without throwing an exception),
     * if any do. Upon normal or exceptional return, tasks that have not completed are cancelled. The results of this method are
     * undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task subject to execution is {@code null}
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> T invokeAnyVirtual(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (shutdown) {
            return null;
        }

        return VIRTUALEXECUTOR.invokeAny(tasks);
    }

    /**
     * Executes the given tasks on virtual threads, returning the result of one that has completed successfully (i.e., without throwing an exception),
     * if any do before the given timeout elapses. Upon normal or exceptional return, tasks that have not completed are cancelled.
     * The results of this method are undefined if the given collection is modified while this operation is in progress
     *
     * @param <T> the type of the values returned from the tasks
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    public static <T> T invokeAnyVirtual(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (shutdown) {
            return null;
        }

        return VIRTUALEXECUTOR.invokeAny(tasks, timeout, unit);
    }

    /**
     * Submits a Runnable task for execution on a virtual thread and returns a {@link Future} representing that task, whose {@link Future#get()} method will return {@code null}
     * upon successful completion
     *
     * @param task the task to submit
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static Future<?> submitVirtual(Runnable task) {
        if (shutdown) {
            return null;
        }

        return VIRTUALEXECUTOR.submit(task);
    }

    /**
     * Submits a Runnable task for execution on a virtual thread and returns a {@link Future} representing that task, whose {@link Future#get()} method will return the given result
     * upon successful completion
     *
     * @param <T> the return type of result
     * @param task the task to submit
     * @param result the result to return
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static <T> Future<T> submitVirtual(Runnable task, T result) {
        if (shutdown) {
            return null;
        }

        return VIRTUALEXECUTOR.submit(task, result);
    }

    /**
     * Submits a value-returning task for execution on a virtual thread and returns a {@link Future} representing the pending results of the task, whose {@link Future#get()} method
     * will return the task's result upon successful completion
     * <p>
     * If you would like to immediately block waiting for a task, you can use constructions of the form {@code result = exec.submit(aCallable).get();}
     * <p>
     * Note: The {@link Executors} class includes a set of methods that can convert some other common closure-like objects, for example,
     * {@link java.security.PrivilegedAction} to {@link Callable} form so they can be submitted
     *
     * @param <T> the return type of the callable
     * @param task the task to submit
     * @return a {@link Future} representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is {@code null}
     */
    public static <T> Future<T> submitVirtual(Callable<T> task) {
        if (shutdown) {
            return null;
        }

        return VIRTUALEXECUTOR.submit(task);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted. Invocation has no additional
     * effect if already shut down
     *
     * @throws SecurityException if a security manager exists and shutting down this ExecutorService may manipulate threads that the caller is not
     * permitted to modify because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}, or the security manager's {@code checkAccess} method denies access.
     */
    public static void shutdown() {
        shutdown = true;
        SCHEDULEDEXECUTOR.shutdown();
        VIRTUALEXECUTOR.shutdown();
    }

    /**
     * Indicates if this ExecutorService is shutdown or in the process of shutting down
     *
     * @return {@code true} if shutdown
     */
    public static boolean isShutdown() {
        return shutdown;
    }
}
