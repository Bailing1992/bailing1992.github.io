package multithread.question1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * describe:有一个压测场景需要三个线程加载完毕资源后再同时发起对目标服务的压测请用java代码实现。
 *
 * @author lichao
 * @date 2019/03/07
 */

public class Test {
    public static void main(String[] args) throws InterruptedException {
        final int threadCount = 3;
        final AtomicInteger count = new AtomicInteger(threadCount);
        final Object waitObject = new Object();
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 0; i < threadCount; i++) {
            final int j = i;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("执行资源加载工作:" + j);
                    synchronized (waitObject) {
                        int cnt = count.decrementAndGet();
                        // 最后一个执行完加载任务的线程，激活所有的等待线程；
                        if (cnt == 0) {
                            waitObject.notifyAll();
                        }else {
                            // 最后一个执行的线程需等待；
                            try {
                                waitObject.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    System.out.println("共同执行压力测试:" + j);
                }
            });
        }
        pool.shutdown();
        Thread.sleep(10000);
        System.out.println("结束线程");
        System.exit(0);

    }
}