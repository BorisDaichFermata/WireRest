package com.wireguard.external.wireguard;

import org.aspectj.weaver.ast.Call;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.*;
import java.util.concurrent.*;

public class BlockingByHashAsyncExecutor<T> {
    final Map<String, Queue<Callable<T>>> tasks = Collections.synchronizedMap(new LinkedHashMap<>());

    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public Future<T> addTask(String hash, Callable<T> task) {
        synchronized (tasks) {
            Queue<Callable<T>> queue = tasks.computeIfAbsent(hash, k -> new LinkedBlockingQueue<>());
            queue.add(task);
        }
        return executor.submit(() -> {
            Queue<Callable<T>> queue = tasks.get(hash);
            synchronized (queue) {
                Callable<T> poll = queue.poll();
                if (poll == null) {
                    tasks.remove(hash);
                    return null;
                }
                return poll.call();
                }

        });
    }



}
