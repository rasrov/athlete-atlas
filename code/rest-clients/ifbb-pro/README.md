# IFBB competition adapter

`IfbbProCompetitionAdapter` (package `com.athlete.atlas.output.rest.ifbb`) implements `CompetitionPort.fetchCompetitions(DateRange)`. It fetches every provider page through `InvokerService.invokePaginated` in the common module. `IfbbEventsClient` performs each page request and converts the response into `RemotePage<T>`; common code has no dependency on IFBB DTOs.

Pagination is sequential and one-based. Page size (default 50) and the page safety limit (default 200) are configured under `rest-clients.ifbb-pro`. All pages must agree on totals and have the expected number of items. Empty catalogs with zero or one reported pages are supported. Null responses, incomplete pages and changing totals fail through the existing RemoteException mapping. No partial result is returned.

Resilience is applied per page in `IfbbEventsClient` (instances `ifbbEventsApi` in `boot/src/main/resources/application.yml`). Only HTTP 429, 5xx and network errors are retried and counted by the circuit breaker; other 4xx fail immediately. When the circuit is open, the request fails with a `RemoteException` (503) without calling the provider.

`DateRange` is half-open: the provider receives `start` and the last included day, and the adapter keeps only competitions overlapping the range. `DateRange.seasonOf(date)` returns the season containing a date, from October 1 to October 1 of the next year. Results are sorted by start date and then by numeric provider ID.

Invalid provider events are discarded, not rejected: missing or non-positive IDs, unparseable or reversed dates, and missing title, description or URL. Duplicate IDs keep the first valid occurrence. Discarded IDs are logged in a single warning per request. Category/division/Natural filtering is not applied.

MapStruct (`IfbbEventMapper`) maps provider DTOs to the existing AthleteCompetition record. Provider date-times are parsed strictly as local dates; text tags are removed and HTML entities decoded. logoUrl defaults to null, since editorial information is not supplied by IFBB; the participation of the athlete comes from the database. IDs use the provisional namespace `ifbb-<sourceId>`; this is not a persistence strategy for provider ID changes.

The client is generated with the `restclient` library into `com.athlete.atlas.output.rest.ifbb.generated`. `IfbbProClientConfiguration` builds its `RestClient` with the configured timeouts and the `User-Agent` required by the contract; the generated API sends `Accept: application/json`.

The generated client models include Map subclasses and object/false/null variants. IfbbJsonConfiguration replaces only this client's Jackson converters with copies (shared converters are never modified) to read model fields as objects and handle missing venue/image variants. Unknown provider properties are ignored. Generated sources are not edited. The two Spring factory methods have distinct bean names.

Run the affected reactor tests from the repository root:

```shell
mvn -f code/pom.xml -pl rest-clients/ifbb-pro -am test
```

Tests do not contact IFBB. Maven generates the client and MapStruct implementation as part of compilation.
