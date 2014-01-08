# sudoku

A demonstration of solving a hard sudoku, slightly adapted from the
example in core.logic's test suite.

## Configuration

First examine the `config.clj` file, which lists 3 components.

### :examples.sudoku

The first component is a custom component that we must provide and tell Jig about in this file.

```clojure
:examples.sudoku
{:jig/component sudoku/Website
 :jig/project "examples/sudoku/project.clj"

 :name "Sudoku"
 :description "A difficult Sudoku grid solved using core.logic. Renders the grid using Hiccup and serves it via Ring."

 :puzzle [[8 0 0 0 0 0 0 0 0]
          [0 0 3 6 0 0 0 0 0]
          [0 7 0 0 9 0 2 0 0]
          [0 5 0 0 0 7 0 0 0]
          [0 0 0 0 4 5 7 0 0]
          [0 0 0 1 0 0 0 3 0]
          [0 0 1 0 0 0 0 6 8]
          [0 0 8 5 0 0 0 1 0]
          [0 9 0 0 0 0 4 0 0]]}
```

The `:jig/component` entry tells Jig where to find the component's code,
which is in `src/sudoku.clj`. We'll look at that code soon. The
`:jig/project` entry tells Jig which Leiningen project to use as the
basis for finding that component - there can have multiple projects
contributing components into a system.

The `:jig/component` and `:jig/project` entries are special because they
are recognized by Jig. The `jig` namespace is reserved for Jig, but you
can use other keys to configure your component in any way you like. All
the entries as a whole, including the special `jig` ones, are passed as
a single argument to the component's constructor.

To see this, let's examine this component's code :-

```clojure
(ns sudoku
  (:require
   jig
   [jig.ring :refer (add-ring-handler)]

   [compojure.core :refer (defroutes GET)])

  (:import (jig Lifecycle)))

(defroutes handler
  (GET "/sudoku.html" ...)

(deftype Website [config]
  Lifecycle
  (init [_ system] (add-ring-handler system config handler))
  (start [_ system] system)
  (stop [_ system] system))
```

The `deftype` declares the component. Any type that satisfies Jig's
`Lifecycle` protocol can be included and configured in a Jig
configuration. The `config` argument allows easy access to the
component's configuration.

The component will be initialized, along we every other component, at
the beginning of every cycle as part of the _reset_ process. Jig calls
the `init` function of every component registered in the system through
the `config.clj` file. The dependency relationships declared influence
the order that components are called: dependencies are initialized
before their dependants. When every component is initialized, Jig
proceeds to start each component up (in the same order as
before). Finally, when a `reset` is request, Jig first stops every
component in reverse order.

Our component calls `add-ring-handler`, a convenience function which
adds the handler to a list held in the system map at a particular
path. The path includes the id of our component to avoid clashing with
the system map entries of other components.

```clojure
(update-in system
  [(:jig/id config) :jig.ring/handlers]
  conj
  (wrap-config handler config))
```

This is just what most Jig components do: they update the system map
with values.

We can add any Ring-compatible handler. In this example, we use
Compojure's `defroutes` which creates a Ring-compatible handler for us.

The `[(:jig/id config) :jig.ring/handlers]` path used is significant,
because the Compojure component we're about to discuss will recognise
it when searching the system map for Ring handlers to amalgamate.

### :examples.sudoku/routing

Let's have a look at the next component. Now we see an example of using
one of the many components that Jig provides _out-of-the-box_. The
`:jig/component` and `:jig/project` entries are there, along with a new
entry labelled `:jig/dependencies`. This entry tells Jig that this
component depends on these other components in the
configuration. Dependencies are very important because they influence
start-up order and allow components to communicate values, via the
system map, to their dependants. Dependencies are always specified as a
vector containing the keys of other components. It is an error if you
declare a dependency on a component which doesn't exist in the elsewhere
in the configuration.

```clojure
:examples.sudoku/routing
{:jig/component jig.compojure/HandlerComposer
 :jig/project "examples/sudoku/project.clj"
 :jig/dependencies [:examples.sudoku]}
```

This is all the configuration the `HandlerComposer` component needs. So
how does it work? When its `init` function is called, the component
iterates over its dependencies looking for ones that have contributed
handlers under the well-known `:jig.ring/handlers` entry in the system
map. These handlers are combined to form a single handler, which the
`HandlerComposer` adds to the system map under another special path:
`:jig.ring/handler` (note we've moved from plural to singular). So you
can see why the `:jig/dependencies` entry is so important - it tells a
component its dependencies and ensures that these dependencies are
initialized ahead of this one.

### :examples.sudoku/webserver

Finally, we have a 3rd component which declares a web sever. This is
another example of using one of Jig's ready-made components. Here is the
configuration for this final component :-

```clojure
:examples.sudoku/webserver
{:jig/component jig.jetty/Server
 :jig/project "examples/sudoku/project.clj"
 :jig/dependencies [:examples.sudoku/routing]
 :port 8091}
```

Note the additional entry specifying the port. Any other entries
(e.g. `join?`) that are recognized by Ring's Jetty server adapter can
also be specified.

We didn't have to use jetty. We can replace `jig.jetty/Server` with
`jig.http-kit/Server`. As long as the Jig extension that provides the
component is specified in this project's Leiningen dependencies, we can
mix and match components.

Jig has been developed with strong opinions on the benefits of
modularity in tackling complexity. But Jig does try to remain impartial
on the question of which Clojure libraries are best suited for your
projects.

## Credits

David Nolen @swannodette for the original Sudoku example.

## Usage

Run lein repl from the Jig project directory. If you haven't already added a configuration file this example will be run instead.

```
> lein repl
> (go)
:ready
```

Then browse to http://localhost:8001

## Copyright and License

Copyright © 2013 JUXT. All rights reserved.

The use and distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file epl-v10.html
at the root of this distribution. By using this software in any fashion,
you are agreeing to be bound by the terms of this license. You must not
remove this notice, or any other, from this software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
