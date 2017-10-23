package com.jukusoft.erp.lib.module;

import com.jukusoft.erp.lib.context.AppContext;
import com.jukusoft.erp.lib.logging.ILogging;
import com.jukusoft.erp.lib.message.request.ApiRequest;
import com.jukusoft.erp.lib.message.response.ApiResponse;
import com.jukusoft.erp.lib.route.Route;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractModule implements IModule {

    //instance of vertx
    protected Vertx vertx = null;

    //instance of module context
    protected AppContext context = null;

    //instance of logger
    protected ILogging logger = null;

    @Override
    public Vertx getVertx() {
        return this.vertx;
    }

    /**
     * get instance of event bus
     *
     * @return instance of event bus
     */
    public EventBus getEventBus () {
        return this.vertx.eventBus();
    }

    @Override
    public ILogging getLogger() {
        return this.logger;
    }

    @Override
    public void init(Vertx vertx, AppContext context, ILogging logger) {
        if (vertx == null) {
            throw new NullPointerException("vertx cannot be null.");
        }

        if (context == null) {
            throw new NullPointerException("app context cannot be null.");
        }

        if (logger == null) {
            throw new NullPointerException("logger cannot be null.");
        }

        this.vertx = vertx;
        this.context = context;
        this.logger = logger;
    }

    protected <T> void addApi (T page) {
        getLogger().debug("search_api_routes", "search for new routes in class " + page.getClass().getSimpleName());

        //get class of object
        Class cls = page.getClass();

        //list all available methods in class
        Method[] methods = cls.getDeclaredMethods();

        for (Method method : methods) {
            /*System.out.println("method: " + method.getName());

            for (Annotation annotation : method.getDeclaredAnnotations()) {
                System.out.println("annotation found: " + annotation.getClass());
            }*/

            //check, if method has annotation @Route
            if (method.isAnnotationPresent(Route.class)) {
                for (String route : method.getAnnotation(Route.class).routes()) {
                    getLogger().debug("module_route_detected", "new route found: " + route + " --> " + cls.getCanonicalName());

                    registerHandler(route, page, method);
                }
            }
        }
    }

    private <T> void registerHandler (String event, T page, Method method) {
        //register handler
        getEventBus().consumer(event, new Handler<Message<ApiRequest>>() {
            @Override
            public void handle(Message<ApiRequest> event) {
                getLogger().debug(event.body().getMessageID(), "consume_message", "consume message: " + event.body());

                //get message
                ApiRequest req = event.body();

                if (method.getReturnType() == ApiResponse.class) {
                    //create new api answer
                    ApiResponse res = new ApiResponse(req.getMessageID());

                    try {
                        res = (ApiResponse) method.invoke(page, event, req, res);

                        getLogger().debug(req.getMessageID(), "reply_message", "reply to message: " + res);

                        //reply to api request
                        event.reply(res);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        event.fail(500, e.getLocalizedMessage());

                        return;
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                        event.fail(500, e.getLocalizedMessage());

                        return;
                    }
                } else {
                    try {
                        method.invoke(page, event, req);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        event.fail(500, e.getLocalizedMessage());
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                        event.fail(500, e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    @Override
    public void start(Handler<Future<IModule>> handler) throws Exception {
        if (handler == null) {
            throw new NullPointerException("start future cannot be null.");
        }

        try {
            this.start();
        } catch (Exception e) {
            e.printStackTrace();
            handler.handle(Future.failedFuture(e));
        }

        handler.handle(Future.succeededFuture(this));
        //startFuture.complete(AbstractModule.this);
    }

    @Override
    public void stop(Handler<Future<Void>> handler) throws Exception {
        try {
            this.stop();
            handler.handle(Future.succeededFuture());
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    /**
     * start module
     *
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * stop module
     *
     * @throws Exception
     */
    public abstract void stop() throws Exception;

}
