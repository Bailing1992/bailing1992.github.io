package chainofResponsibility.handlerMethod;

import chainofResponsibility.HandlerMethod;
import chainofResponsibility.entity.User;

/**
 * describe:
 *
 * @author lichao
 * @date 2019/03/07
 */
public class OpenFingerprintHandlerMethod implements HandlerMethod {

    @Override
    public void handler(Object input) {
        if(input instanceof User){
            System.out.println("User:" + ((User) input).getName() + ":开始执行用户开通指纹识别功能 ....");
        }
    }
}
