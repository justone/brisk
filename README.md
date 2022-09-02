# brisk

Freeze and thaw with [Nippy](https://github.com/ptaoussanis/nippy) at the command line.

# Install

Download the latest from the [releases page](https://github.com/justone/brisk/releases).

# Usage

Freeze data:

```
brisk --freeze -i data.edn -o data.nippy
```

Thaw data:

```
brisk --thaw -i data.nippy -o data.edn
```

If input or output is not specified, stdin or stdout will be used:

```
cat data.edn | brisk -f | brisk -t > data2.edn
```

# [Babashka pod](https://github.com/babashka/babashka.pods) support

There are two functions exposed via the pod interface:

* `(freeze-to-file filename data)` - returns the number of bytes written
* `(thaw-from-file filename)` - returns data thawed from the file
* `(freeze-to-string data)` - returns frozen data as an encoded string
* `(thaw-from-string encoded)` - returns data thawed from the encoded string

Example:

```
#!/usr/bin/env bb

(require '[babashka.pods :as pods])
(pods/load-pod "brisk")
(require '[pod.brisk :as brisk])

(brisk/freeze-to-file "pod.nippy" {:han :solo})
(prn (brisk/thaw-from-file "pod.nippy"))
```

# Development

Not quite ready yet. This depends on a soon-to-be-released library.

# Things that don't work

* Embedded objects - Nippy can handle them, but Graal VM does not support them.

# License

Copyright © 2020-2022 Nate Jones

Distributed under the EPL License. See LICENSE.
