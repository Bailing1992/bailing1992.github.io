package chainofResponsibility;

/**
 * describe:Handler execution chain, consisting of handler object and any handler interceptors.
 * 仿照SpringMVC中的代码
 * @author lichao
 * @date 2019/03/07
 */
public interface  HandlerInterceptor {
    default boolean preHandle(Object user) throws Exception {
        return true;
    }
}
