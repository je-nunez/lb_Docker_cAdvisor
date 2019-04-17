# lb_Docker_cAdvisor

Agent that parses input metrics from cAdvisor
[https://github.com/google/cadvisor](https://github.com/google/cadvisor)
about a Docker service cluster,
and then summarizes such input metrics into a single scalar value to implement
Dynamic Weighted Round Robin for a LB.

(Another option where to take the input metrics would be to take the
metrics from the Docker Engine API directly,
[https://docs.docker.com/engine/api/v1.21/](https://docs.docker.com/engine/api/v1.21/),
-e.g., the query `GET /containers/(id or name)/stats`-, or to take them from
monitoring systems like Prometheus.)

# WIP

This project is a *work in progress*. The implementation is *incomplete* and
subject to change. The documentation can be inaccurate.

# How to Run:

Run Maven:

        mvn

By default, it queries the cAdvisor at `localhost:8080` address. (In fact, the
program accepts the hostname/IP address of the cAdvisor server as the first
command-line argument -add this address explicitly in the Maven `pom.xml` in
this project, for the corresponding `argument` element. To change the port name
to query cAdvisor from the default of 8080 also is easily doable in the next
`argument` element in the `pom.xml` file.)

# Notes

cAdvisor returns multiple stats (under the "stats" subtree) for each Docker
container, including a timestamp for each complete measure (see the `timestamp`
at the bottom) for it returns multiple sets of measures for each Docker
container. An example is below (the JSON from cAdvisor has been very
abbreviated):

      {
          "/docker/cfc0fb6f62f9cc35b38d5d8647667739e7efb90e15b4bab869f34682387be0d7": {
              "aliases": [ ... ],
              ...,
              "stats": [    <<<--- note that "stats" is an array
               {
                  "cpu": {
                    "load_average": 0,
                    "usage": {
                        ...,
                        "system": 1410360000000,
                        "total": 2895301641397,
                        "user": 1138870000000
                    }
                },
                "diskio": { ... },
                "memory": {
                    "cache": 65536,
                    ...,
                    "max_usage": 84123648,
                    "rss": 73531392,
                    "swap": 0,
                    "usage": 73707520,
                    "working_set": 73674752
                },
                "network": { ... },
                ...,
                "timestamp": "2018-10-15T00:02:55.142234734Z"
            },
            ... more complete "stats" samples from cAdvisor about this same
            ... Docker container

