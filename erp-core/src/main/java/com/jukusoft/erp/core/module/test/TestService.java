package com.jukusoft.erp.core.module.test;

import com.jukusoft.erp.lib.annotation.LoginRequired;
import com.jukusoft.erp.lib.message.request.ApiRequest;
import com.jukusoft.erp.lib.message.response.ApiResponse;
import com.jukusoft.erp.lib.route.Route;
import com.jukusoft.erp.lib.service.AbstractService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

public class TestService extends AbstractService {

    @Route(routes = "/")
    public ApiResponse homePage (Message<ApiRequest> event, ApiRequest req, ApiResponse res) {
        res.getData().put("content", "Hi! This is an internal api.");

        return res;
    }

    @LoginRequired
    @Route(routes = {"/metrics", "/metrics/"})
    public void metrics (Message<ApiRequest> event, ApiRequest req, Handler<AsyncResult<ApiResponse>> handler) {
        //send new api answer
        ApiResponse res = new ApiResponse(req.getMessageID(), req.getSessionID(), req.getEvent());

        res.getData().put("content", "Metrics");

        //reply to api request
        event.reply(res);
    }

}