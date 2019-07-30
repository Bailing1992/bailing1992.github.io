package chainofResponsibility.handlerInterceptorImpl;

import chainofResponsibility.HandlerInterceptor;

import java.util.Random;

/**
 * describe:
 *
 * @author lichao
 * @date 2019/03/07
 */
public class HandlerInterceptorC implements HandlerInterceptor {
    @Override
    public boolean preHandle(Object o) throws Exception {
        Random random = new Random();
        boolean isSuccess = random.nextBoolean();
        System.out.println("执行过滤器 C --- "+ (isSuccess ? "执行通过":"执行未通过"));
        return isSuccess ? true : false;
    }
}
