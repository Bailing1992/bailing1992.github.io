package chainofResponsibility;

import chainofResponsibility.entity.User;
import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * describe:Handler execution chain, consisting of handler object and any handler interceptors.
 * 仿照SpringMVC中的代码 *
 * @author lichao
 * @date 2019/03/07
 */
public class HandlerExecutionChain {

    private final Object handler;

    @Nullable
    private HandlerInterceptor[] interceptors;

    @Nullable
    private List<HandlerInterceptor> interceptorList;

    /**
     * Create a new HandlerExecutionChain.
     * @param handler the handler object to execute
     */
    public HandlerExecutionChain(Object handler) {
            this.handler = handler;
    }

    /**
     * Return the handler object to execute.
     */
    public Object getHandler() {
        return this.handler;
    }

    /**
     * 添加interceptor时，会把HandlerInterceptors数组清空,以便下次重新生成；
     */
    public void addInterceptor(HandlerInterceptor interceptor) {
        initInterceptorList().add(interceptor);
        this.interceptors = null;

    }


    private List<HandlerInterceptor> initInterceptorList() {
        if (this.interceptorList == null) {
            this.interceptorList = new ArrayList<>();
        }
        return this.interceptorList;
    }

    /**
     * Apply preHandle methods of registered interceptors.
     * @return {@code true} if the execution chain should proceed with the
     * next interceptor or the handler itself. Else, DispatcherServlet assumes
     * that this interceptor has already dealt with the response itself.
     */
    boolean applyPreHandle(User user) throws Exception {
        HandlerInterceptor[] interceptors = getInterceptors();
        if (!(interceptors == null || interceptors.length==0)) {
            for (int i = 0; i < interceptors.length; i++) {
                HandlerInterceptor interceptor = interceptors[i];
                if (!interceptor.preHandle(user)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return the array of interceptors to apply (in the given order).
     * @return the array of HandlerInterceptors instances (may be {@code null})
     */
    @Nullable
    public HandlerInterceptor[] getInterceptors() {
        if (this.interceptors == null && this.interceptorList != null) {
            this.interceptors = this.interceptorList.toArray(new HandlerInterceptor[0]);
        }
        return this.interceptors;
    }



}
