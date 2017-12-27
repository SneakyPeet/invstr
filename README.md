# invstr

Helper to scrape latest strategy data from invstr.io

`lein run username <invstr-username> password <invstr-password> strat-id <za-rookie|za-adventurer|za-maveric>`

Writes the data to `/data/<strat-id>/<post-title>.edn`

prints something like


```
Rookie: 2017-12-21 Trade

Current Assets (# of stocks)  12
Current Exposure (% of funds invested)  56.1 %

|      :date | :stockcode | :allocation | :close |
|------------+------------+-------------+--------|
| 2017-12-21 |        AAA |        4.4% |  35610 |
| 2017-12-21 |        BBB |        4.8% |  56400 |
| 2017-12-21 |        CCC |        5.2% |  72619 |
| 2017-12-21 |        DDD |        5.4% | 217001 |
| 2017-12-21 |        EEE |        5.0% |  15672 |
| 2017-12-21 |        FFF |        4.3% |   4183 |
| 2017-12-21 |        GGG |        4.6% |   9358 |
| 2017-12-21 |        HHH |        4.3% |  21941 |
| 2017-12-21 |        III |        3.5% | 115067 |
| 2017-12-21 |        JJJ |        4.7% |   8320 |
| 2017-12-21 |        KKK |        4.7% |  30615 |
| 2017-12-21 |        LLL |        5.3% |   1820 |

```
