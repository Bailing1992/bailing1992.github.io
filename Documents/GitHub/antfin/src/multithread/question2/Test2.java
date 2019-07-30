package multithread.question2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * describe:用户在支付时候需要核身平台对可用的核身产品进行分析，假设现有指纹、人脸两个服务提供对应产品可用性判断逻辑，
 * 现在希望平台可以更高快速度调用系统完成生成各产品可用性决策汇总，请用java代码实现（注意下游系统可能出现宕机问题）？
 *
 * @author lichao
 * @date 2019/03/07
 */
public class Test2 {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final ExecutorService pool = Executors.newCachedThreadPool();
        final ArrayList<Callable<Result>> checkProducts = new ArrayList<>(); // 需要判断产品可用性的服务列表；
        Param param = new Param("lichao"); // param用于模拟服务可用性判断时的输入参数；
        checkProducts.add(()->(new FingerprintCheckProduct()).isAbailable(param));
        checkProducts.add(()-> (new FaceCheckProduct()).isAbailable(param));
        // 并发执行所有服务提供的对应产品可用性判断逻辑；设置超时时间为100毫秒（即认为服务不可用）；
        List<Future<Result>> rets = pool.invokeAll(checkProducts ,100, TimeUnit.MILLISECONDS);
        // 生成各产品可用性决策汇总信息；
        for(Future<Result> ret : rets){
            try{
                if (ret.get().isStatus() == true) {
                    System.out.println(ret.get().getDesc());
                } else {
                    System.out.println(ret.get().getDesc());
                }
            }catch ( CancellationException e){
                System.out.println("服务超时，cancel");
                ret.cancel(true);
            }
        }
        pool.shutdown();
        Thread.sleep(500);
        System.out.println("结束线程");
        System.exit(0);

    }

    /**
     *
     * describe: 模拟实现了第三房服务提供的可用性判断逻辑的接口；
     *
     * @auther: lichao
     * @date: 2019/3/7 3:11 PM
     */
    @FunctionalInterface
    public interface CheckProduct {
        Result isAbailable(Param param) throws InterruptedException;
    }

    /**
     *
     * describe:模拟实现了指纹服务提供的可用性判断逻辑的实现；
     *
     * @auther: lichao
     * @date: 2019/3/7 3:13 PM
     */
    public static class FingerprintCheckProduct implements CheckProduct{
        @Override
        public Result isAbailable(Param param) throws InterruptedException {
            Random random = new Random();
            int sleepMillisTime = random.nextInt(150); //模拟逻辑执行时间
            System.out.println("FingerprintCheckProduct 服务执行时间：" + sleepMillisTime);
            Thread.sleep(sleepMillisTime);
            return random.nextBoolean() == true ?
                    new Result(true, "user:" + param.getUser() + "--支持指纹识别")
                    :
                    new Result(true, "user:" + param.getUser() + "--不支持指纹识别");
        }
    }

    /**
     *
     * describe:模拟实现了人脸识别服务提供的可用性判断逻辑的实现；
     *
     * @auther: lichao
     * @date: 2019/3/7 3:13 PM
     */
    public static class FaceCheckProduct implements CheckProduct{
        @Override
        public Result isAbailable(Param param) throws InterruptedException {
            Random random = new Random();
            int sleepMillisTime = random.nextInt(150); //模拟逻辑执行时间
            System.out.println("FaceCheckProduct 服务执行时间："+ sleepMillisTime);
            Thread.sleep(sleepMillisTime);
            return random.nextBoolean() == true ?
                    new Result(true, "user:" + param.getUser() + "--支持人脸识别")
                    :
                    new Result(true, "user:" + param.getUser() + "--不支持人脸识别");
        }
    }



    /**
     *
     * describe: 第三方核身产品服务返回的可用性分析结果类；
     *
     * @auther: lichao
     * @date: 2019/3/7 2:07 PM
     */
    public static class Result {
        private boolean status ;
        private String desc;
        public Result(boolean status, String desc){
            this.status = status;
            this.desc  = desc;
        }
        public String getDesc() {
            return desc;
        }
        public boolean isStatus() {
            return status;
        }
    }

    /**
     *
     * describe: 访问第三方核身产品服务时的请求参数；
     *
     * @param:
     * @return:
     * @auther: lichao
     * @date: 2019/3/7 2:09 PM
     */
    public static class Param{

        private final String user;

        Param(String user){
            this.user = user;
        }

        public String getUser() {
            return user;
        }

    }

}