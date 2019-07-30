package chainofResponsibility;


import chainofResponsibility.entity.User;
import chainofResponsibility.handlerInterceptorImpl.HandlerInterceptorA;
import chainofResponsibility.handlerInterceptorImpl.HandlerInterceptorB;
import chainofResponsibility.handlerInterceptorImpl.HandlerInterceptorC;
import chainofResponsibility.handlerMethod.OpenFingerprintHandlerMethod;

/**
 * describe: 思路与SpringMVC中的执行链一致；
 *
 * @author lichao
 * @date 2019/03/07
 */
public class Bootstrap {
    public static void main(String [] args) throws Exception {

        HandlerMethod handler = new OpenFingerprintHandlerMethod();
        HandlerInterceptor handlerInterceptorA = new HandlerInterceptorA();
        HandlerInterceptor handlerInterceptorB = new HandlerInterceptorB();
        HandlerInterceptor handlerInterceptorC = new HandlerInterceptorC();
        // 严格执行链
        HandlerExecutionChain strictHandlerExecutionChain= new HandlerExecutionChain(handler);
        strictHandlerExecutionChain.addInterceptor(handlerInterceptorA);
        strictHandlerExecutionChain.addInterceptor(handlerInterceptorB);
        strictHandlerExecutionChain.addInterceptor(handlerInterceptorC);

        // 宽松执行链
        HandlerExecutionChain looseHandlerExecutionChain= new HandlerExecutionChain(handler);
        looseHandlerExecutionChain.addInterceptor(handlerInterceptorA);
        looseHandlerExecutionChain.addInterceptor(handlerInterceptorB);

        System.out.println("严格：");
        User user = new User("lichao","188888888888");
        if (strictHandlerExecutionChain.applyPreHandle(user)){
            if(strictHandlerExecutionChain.getHandler() instanceof HandlerMethod){
                ((HandlerMethod) strictHandlerExecutionChain.getHandler()).handler(user);
            }
        }
        System.out.println("宽松：");
        User user1 = new User("lichao1","188888888888");
        if (looseHandlerExecutionChain.applyPreHandle(user1)){
            if(looseHandlerExecutionChain.getHandler() instanceof HandlerMethod){
                ((HandlerMethod) strictHandlerExecutionChain.getHandler()).handler(user1);
            }
        }
    }
}
