# mqtt-broker

A demonstration of a jig running an MQTT server using Netty.

## Configuration

First examine the `config.clj` file, which lists 4 components.

The first 2 components are provided by Jig's netty-mqtt extension, which provides Netty encoder and decoder components.

The third component is a custom component provided in the example's
source directory. It registers a function under a well-known key,
`:jig.netty/handler-factory`. The function gets called with no
arguments, and provides a Netty ```io.netty.channel.ChannelHandler```.

Thanks to the supplied codec (provided by Xively's clj-mqtt project) the
actual protocol implementation is rather trivial.

```clojure
     #(proxy [ChannelHandlerAdapter] []
        (channelRead [ctx msg]
          (case (:type msg)
            :connect (reply ctx {:type :connack})
            :pingreq (reply ctx {:type :pingresp})
            :publish (println (format "PUBLISH MESSAGE: topic is %s, payload is '%s'" (:topic msg) (String. (:payload msg))))
            :disconnect (.close ctx))))))

```

## Usage

Run lein repl from the Jig project directory.

1. Create a symlink from `$HOME/.jig/config.clj` to the `config.clj` in this directory.

```
$ mkdir ~/.jig
$ cd ~/.jig
$ ln -s ~/src/jig/examples/mqtt-broker/config.clj
```

2. Cd into the Jig directory

```
$ cd ~/src/jig
```

3. Start Jig

```
$ lein repl
nREPL server started on port 41713 on host 127.0.0.1
REPL-y 0.3.0
Clojure 1.5.1
Welcome to Jig!

(go)       -- start the system
(reset)    -- reset the system
(refresh)  -- recover if a reset fails due to a compilation error
(menu)     -- show this menu again
Jig user> (go)
:ready
```

4. In a new terminal window, download and install [mosquitto](http://mosquitto.org/download/)

5. Test the MQTT broker by publishing messages.

Use mosquitto to publish some test messages.

```
mosquitto_pub -h localhost -t "/foo" -m "message"
```

You should see messages printed out on Jig's standard out.

## Discussion

This example should give you a feeling for how easy it is to integrate
different components to form systems.

None of the components in Jig are more than a few lines of code, but
when they are combined the result is both powerful and extremely flexible.

## Credits

Thanks to Xively, and in particular Paul Bellamy @pyrhho and Martin Trojer
@martintrojer for the Netty MQTT codec used in this example.

## Copyright and License

Copyright © 2014 JUXT. All rights reserved.

The use and distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file epl-v10.html
at the root of this distribution. By using this software in any fashion,
you are agreeing to be bound by the terms of this license. You must not
remove this notice, or any other, from this software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
