package finance.handlers

import ratpack.handling.Handler
import ratpack.handling.Context

class CorsHandler implements Handler {

    void handle(Context ctx) {
        ctx
                .response
                .headers
                .add('Access-Control-Allow-Origin', '*')
                .add('Access-Control-Allow-Methods', 'GET, POST,DELETE,PUT')
                .add('Access-Control-Allow-Headers', 'Content-Type, Authorization')

        ctx.next()
    }
}