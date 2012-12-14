# lein-partest

A Leiningen plugin to test for parallelism

## Usage

This plugin can be used on a project or user level.

Put `[lein-partest "0.1.0"]` into the `:plugins` vector of your
project.clj or your `:user` profile accordingly.

## Invokation

    $ lein partest <qualified-fn-sym> <parallel-runs> [<warmup-runs>]

`qualified-fn-sym` names a function taking one numeric argument which is its run number.

e.g. to test with a parallelism of 4 and a default of two warmup runs

    $ lein partest ns.example/test-ns 4

## License

Copyright © 2012 Herwig Hochleitner

Distributed under the Eclipse Public License, the same as Clojure.
