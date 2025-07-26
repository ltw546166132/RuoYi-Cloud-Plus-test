package org.dromara.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 线程相关工具类.
 *
 * @author ruoyi
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Threads {

    /**
     * 停止线程池
     * 先使用shutdown, 停止接收新任务并尝试完成所有已存在任务.
     * 如果超时, 则调用shutdownNow, 取消在workQueue中Pending的任务,并中断所有阻塞函数.
     * 如果仍然超時，則強制退出.
     * 另对在shutdown时线程本身被调用中断做了处理.
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                        log.info("Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 打印线程异常信息
     */
    public static void printException(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            log.error(t.getMessage(), t);
        }
    }public static <T> void processInParallel(List<T> list, java.util.function.Consumer<T> task, Executor threadPoolTaskExecutor) {
        if (CollUtil.isEmpty(list)) return;
        if (ObjectUtil.isNull(task)) return;
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (T item : list) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                task.accept(item);
            }, threadPoolTaskExecutor);
            completableFutures.add(future);
        }
        if(CollUtil.isNotEmpty(completableFutures)){
            // 等待所有任务完成
            CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
        }
    }

    public static <T, V> List<V> processInParallel(List<T> list, java.util.function.Function<T, V> task, Executor threadPoolTaskExecutor) {
        if (CollUtil.isEmpty(list)) return null;
        if (ObjectUtil.isNull(task)) return null;
        List<CompletableFuture<V>> completableFutures = new ArrayList<>();
        for (T item : list) {
            CompletableFuture<V> future = CompletableFuture.supplyAsync(() -> {
                V v = null;
                v = task.apply(item);
                return v;
            }, threadPoolTaskExecutor);
            completableFutures.add(future);
        }
        List<V> results = completableFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        return results;
    }


}
