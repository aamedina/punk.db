# net.wikipunk/punk.db
Goal: Bootstrap dev-local Datomic databases from loaded RDF models.

## net.wikipunk.datomic/Connection
Configure a connection to a dev-local Datomic database in the system.

``` clojure
{:vocab  {:sc/create-fn net.wikipunk.rdf/map->UniversalTranslator
          :init-ns      net.wikipunk.mop.init
          :ns-prefix    "net.wikipunk.rdf."
          :boot         [net.wikipunk.punk.db.boot/punk.db]}
 :openai {:sc/create-fn net.wikipunk.openai/map->Client}
 :client {:sc/create-fn datomic.client.api/client
          :server-type  :dev-local
          :system       "dev"}
 :rdf    {:sc/create-fn net.wikipunk.datomic/map->Connection
          :sc/refs      [:client]
          :db-name      "punk.db"}}
```

This is a [schematic](https://github.com/walmartlabs/schematic)
configuration map which is assembled and started using
[component](https://github.com/stuartsierra/component).

## :dev

``` shell
clojure -A:dev
```

``` clojure
(reset)
```

## License
Copyright (c) 2023 Adrian Medina

Permission to use, copy, modify, and/or distribute this software for
any purpose with or without fee is hereby granted, provided that the
above copyright notice and this permission notice appear in all
copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
