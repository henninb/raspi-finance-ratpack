package finance.handlers

import ratpack.core.handling.Handler
import ratpack.core.handling.Context
import ratpack.core.http.HttpMethod
import ratpack.core.http.Request

import java.net.http.HttpResponse

class CorsHandler implements Handler {

    void handle(Context context) {
        context
                .response
                .headers
                .add('Access-Control-Allow-Origin', '*')
                .add('Access-Control-Allow-Methods', 'OPTIONS, GET, POST, DELETE, PUT')
                .add('Access-Control-Allow-Headers', 'Origin, Content-Type, Accept, Authorization, x-requested-with, content-type')
        context.next()
    }
}