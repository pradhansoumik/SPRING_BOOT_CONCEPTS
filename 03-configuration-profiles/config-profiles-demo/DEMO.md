# Config & profiles demo

**Theory:** [`../CONFIG-AND-PROFILES.md`](../CONFIG-AND-PROFILES.md)

```bash
# defaults only (application.yml)
mvn -f 03-configuration-profiles/config-profiles-demo/pom.xml spring-boot:run

# activate dev → application-dev.yml overlays
mvn -f 03-configuration-profiles/config-profiles-demo/pom.xml spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

No web server. Watch `========== config ==========`.

| Run | `url` | `FakePayment` bean |
|---|---|---|
| no profile | `http://default-pay` | `false` (`fake-payment: false`) |
| `dev` | `http://localhost:9999` | `true` |

CLI still wins: `--app.payment.url=http://cli` on top of `dev`.

```text
Environment (run step 3)
    application.yml
        + application-dev.yml   if profile dev
            + CLI / env
refresh()
    bind PaymentProps
    @ConditionalOnProperty → FakePaymentMarker or skip
ApplicationRunner prints
```
