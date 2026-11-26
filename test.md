# MediRDV: step-by-step demo and testing guide

This guide is for demonstrating the service contracts and the complete appointment workflow. It assumes Docker Desktop is running and the Compose stack is started from the repository root.

## 0. Start and check the stack

```powershell
docker compose up -d --build
docker compose ps
```

Wait until `postgres`, `patient-service`, `praticien-service`, `assurance-legacy`, and `rendezvous-service` show healthy. `pgadmin` does not have a healthcheck in this Compose file.

Useful service URLs:

| Service | REST / SOAP | Swagger / contract |
|---|---|---|
| Patient | `http://localhost:8081` | `http://localhost:8081/swagger-ui.html` |
| Praticien | REST `http://localhost:8082`; gRPC `localhost:9090` | `http://localhost:8082/swagger-ui.html` |
| Assurance legacy | SOAP `http://localhost:8080/ws` | WSDL `http://localhost:8080/ws/assurances.wsdl` |
| Rendezvous | `http://localhost:8083` | `http://localhost:8083/swagger-ui.html` |

The OpenAPI documents are available at `/openapi.yaml` on ports 8081, 8082, and 8083.

---

## 1. Patient service: REST check

Open Swagger at `http://localhost:8081/swagger-ui.html`, or use PowerShell:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
$patients = Invoke-RestMethod http://localhost:8081/api/v1/patients
$patients | Format-Table id, nom, numeroAssure
```

Choose a patient with a covered insurance number, for example `ASSURE-001`. Record the returned patient `id`; the seed IDs can differ if the database volume already contained data.

You can also fetch one patient by ID:

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/patients/1
```

Expected: HTTP 200 and a JSON patient record. The rendezvous service later calls this endpoint itself.

---

## 2. SOAP: explain the contract and call the service

### What to show the teacher

- The contract is the XSD file [`contracts/assurance.xsd`](contracts/assurance.xsd). It defines the XML request and response elements, their namespace, and data types.
- The assurance server also has a copy at [`assurance-legacy/src/main/resources/xsd/assurance.xsd`](assurance-legacy/src/main/resources/xsd/assurance.xsd).
- [`assurance-legacy/src/main/java/com/medirdv/assurancelegacy/config/WebServiceConfig.java`](assurance-legacy/src/main/java/com/medirdv/assurancelegacy/config/WebServiceConfig.java) publishes the SOAP servlet at `/ws/*` and generates the WSDL from the XSD. Show the generated WSDL at `http://localhost:8080/ws/assurances.wsdl`.
- [`AssuranceEndpoint.java`](assurance-legacy/src/main/java/com/medirdv/assurancelegacy/endpoint/AssuranceEndpoint.java) maps SOAP payloads to Java methods using `@PayloadRoot`, namespace `http://medirdv.com/assurance-legacy`, and element names such as `verifierDroitsRequest`.
- Maven JAXB plugins generate Java classes from the XSD. The rendezvous client has its own generated JAXB classes from the same shared contract.

### Verify coverage in Postman

Create a request:

- Method: `POST`
- URL: `http://localhost:8080/ws`
- Header: `Content-Type: text/xml; charset=utf-8`
- Body: raw XML

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ass="http://medirdv.com/assurance-legacy">
  <soapenv:Header/>
  <soapenv:Body>
    <ass:verifierDroitsRequest>
      <ass:numeroAssure>ASSURE-001</ass:numeroAssure>
    </ass:verifierDroitsRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Expected: a SOAP response containing `verifierDroitsResponse`, `couverture` (`true`) and `taux` (for the seeded record, `70.00`). Try `ASSURE-006` to see a valid SOAP response with no coverage. An unknown number produces a SOAP **Client** fault (invalid request/business input); SOAP 1.1 transports faults with HTTP 500, which is expected and does not mean the server crashed. Rendezvous translates that known SOAP business fault to HTTP 422 when booking.

PowerShell alternative (prints the XML response):

```powershell
$soap = @'
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ass="http://medirdv.com/assurance-legacy"><soapenv:Header/><soapenv:Body><ass:verifierDroitsRequest><ass:numeroAssure>ASSURE-001</ass:numeroAssure></ass:verifierDroitsRequest></soapenv:Body></soapenv:Envelope>
'@
Invoke-WebRequest http://localhost:8080/ws -Method Post -ContentType "text/xml; charset=utf-8" -Body $soap
```

### SOAP call flow

```text
HTTP POST /ws with SOAP Envelope
  -> MessageDispatcherServlet
  -> AssuranceEndpoint.verifierDroits (@PayloadRoot)
  -> AssuranceService / PostgreSQL
  -> JAXB-marshalled SOAP response
```

There are two contract operations: `verifierDroits` (check coverage) and `declarerConsultation` (declare a completed visit and return an acknowledgement).

---

## 3. Praticien REST and gRPC server

### REST side

Open `http://localhost:8082/swagger-ui.html`, or run:

```powershell
$praticiens = Invoke-RestMethod http://localhost:8082/api/v1/praticiens
$praticiens | Format-Table id, nom, specialite
```

REST is used for practitioner and slot administration, and for retrieving consultation metadata. The rendezvous service uses this REST client to obtain practitioner/date details when closing a rendezvous.

### What to show about gRPC

- The shared protobuf contract is [`contracts/medirdv.praticien.v1.proto`](contracts/medirdv.praticien.v1.proto). It defines the `PraticienService` service, RPC methods, request/response messages, and field numbers.
- The server implementation is [`PraticienGrpcService.java`](praticien-service/src/main/java/com/medirdv/praticien/grpc/PraticienGrpcService.java), registered with `@GrpcService`. It extends generated `PraticienServiceImplBase`.
- Maven runs `protoc` and the gRPC Java plugin to generate Java message and service classes. Generated files are under `target/generated-sources`; they are build output, not the source contract.
- The server listens on port `9090`. Rendezvous connects inside Docker to `praticien-service:9090`; Compose also publishes it as `localhost:9090` so Postman on the host can connect.
- The three RPCs are `ListerCreneauxDisponibles` (server streaming), `ReserverCreneau` (unary), and `LibererCreneau` (unary compensation operation).

### Test gRPC in Postman

Port `9090` is a gRPC endpoint, not an HTTP REST endpoint. In Postman create a **gRPC** request (not an HTTP request):

1. Select **New** → **gRPC**.
2. Set the server URL to `localhost:9090`.
3. Keep TLS disabled / use plaintext. This local Compose gRPC listener does not use TLS.
4. Open the service definition or method selector. Reflection is enabled by the gRPC server, so Postman should discover `medirdv.praticien.v1.PraticienService`. If discovery does not work, open the Service definition tab and import [`contracts/medirdv.praticien.v1.proto`](contracts/medirdv.praticien.v1.proto).
5. Choose `ListerCreneauxDisponibles`, set the message body, then click **Invoke**. This is server streaming; Postman displays each returned slot.

Example request message (set `du` to the next day at midnight and `au` to the day after at midnight; these are local date-times with no timezone). The seed dates are generated when praticien-service initializes, so if the stack was first started on another date, choose a range covering the actual seeded slots:

```json
{
  "specialite": "CARDIOLOGIE",
  "du": "2026-11-27T00:00:00",
  "au": "2026-11-28T00:00:00"
}
```

If there are no results, adjust the date range to a future date or leave `specialite`, `du`, and `au` empty to list all free slots. Copy a returned `id` for the unary reservation demo below. The schema defines `du` as inclusive and `au` as exclusive.

To test the unary `ReserverCreneau` method, select it and send this message with a currently free slot ID and a fresh UUID:

```json
{
  "requestId": "4e0e4fc3-29b9-4823-952f-94cb1af78260",
  "creneauId": 123
}
```

Replace `creneauId` with the copied slot ID. Expected response: the same slot ID, its practitioner ID, and `dejaTraite: false`. Invoke the exact request again: expect `dejaTraite: true` (idempotent replay). A different UUID against the already reserved slot should return `FAILED_PRECONDITION`.

**Clean up the direct reservation demo:** invoke `LibererCreneau` with the same slot ID and a different fresh UUID:

```json
{
  "requestId": "b43586aa-f592-4145-80fd-c5f8c6d974ce",
  "creneauId": 123
}
```

Expected: a response containing the slot ID. Confirm in praticien Swagger that the slot is `LIBRE` again. Do not use this cleanup step for a slot already booked through rendezvous unless you also cancel the corresponding rendezvous; for the end-to-end flow, use a separate free slot.

Postman’s gRPC request requires a service definition, which can come from server reflection or an imported `.proto` file; see [Postman: manage service definitions for gRPC requests](https://learning.postman.com/latest-v-12/docs/use/send-requests/protocols/grpc/using-service-definition) and [Postman: invoke a gRPC request](https://learning.postman.com/docs/use/send-requests/protocols/grpc/first-grpc-request/).

### Call the gRPC server from the terminal

If `grpcurl` is installed and port 9090 is reachable from your machine, list reflected services:

```powershell
grpcurl -plaintext localhost:9090 list
```

If `grpcurl` is installed and port 9090 is reachable from your machine, use the local command above. Otherwise, invoke grpcurl from a temporary container on the Compose network (this downloads an image if it is not already cached):

```powershell
docker run --rm --network medirdv_default fullstorydev/grpcurl:latest -plaintext praticien-service:9090 list
```

List available cardiology slots for tomorrow (change the date if needed):

```powershell
docker run --rm --network medirdv_default fullstorydev/grpcurl:latest -plaintext `
  -d '{"specialite":"CARDIOLOGIE","du":"2026-11-27T00:00:00","au":"2026-11-28T00:00:00"}' `
  praticien-service:9090 medirdv.praticien.v1.PraticienService/ListerCreneauxDisponibles
```

Expected: zero or more streamed `Creneau` messages containing `id`, practitioner details, and `debut` / `fin`. If none are returned, pick a future date range or use the REST availability step in section 5. The seed slots are generated relative to the day the praticien seed runs.

To call reservation directly, replace `SLOT_ID` with a currently free ID and use a new UUID:

```powershell
docker run --rm --network medirdv_default fullstorydev/grpcurl:latest -plaintext `
  -d '{"requestId":"4e0e4fc3-29b9-4823-952f-94cb1af78260","creneauId":"SLOT_ID"}' `
  praticien-service:9090 medirdv.praticien.v1.PraticienService/ReserverCreneau
```

Expected: response with `creneauId`, `praticienId`, and `dejaTraite:false`. Repeat with the same request ID to demonstrate idempotency (`dejaTraite:true`). A second, different request ID for the now-reserved slot should return a gRPC `FAILED_PRECONDITION` error. For the full rendezvous demo, do not reserve the slot directly first; let rendezvous reserve it in section 5.

### gRPC call flow

```text
RendezvousService
  -> PraticienGrpcClient (generated blocking stub + deadline)
  -> gRPC channel to praticien-service:9090
  -> generated server base dispatches RPC
  -> PraticienGrpcService
  -> GrpcRequestService / repositories / PostgreSQL
  -> protobuf response or gRPC status error
```

---

## 4. Rendezvous REST API on its own

Open `http://localhost:8083/swagger-ui.html`. Useful endpoints:

- `GET /api/v1/rendezvous` — list records; optional `patientId` filter.
- `GET /api/v1/rendezvous/{id}` — retrieve one record.
- `GET /api/v1/rendezvous/creneaux-disponibles?patientId={id}` — return free slots through the gRPC client, excluding slots already active for that patient.
- `POST /api/v1/rendezvous` — orchestrate patient, insurance, and practitioner calls.
- `PATCH /api/v1/rendezvous/{id}/statut` — cancel or close a rendezvous.

Before booking, obtain a fresh available slot. Never rely on a fixed slot ID because database IDs depend on the existing volume and seed history.

```powershell
$patientId = 1 # replace with a covered patient ID from section 1
$available = Invoke-RestMethod "http://localhost:8083/api/v1/rendezvous/creneaux-disponibles?patientId=$patientId&specialite=CARDIOLOGIE"
$available | ConvertTo-Json -Depth 8
```

Pick an `id` from the returned slot list. If the result is empty, omit the specialty filter or choose another future date/specialty in Swagger.

---

## 5. End-to-end booking: rendezvous calls all services

This is the main integration demonstration. Use a patient whose insurance is covered (for example, the patient whose `numeroAssure` is `ASSURE-001`) and a free slot ID just returned by section 4.

In rendezvous Swagger (`http://localhost:8083/swagger-ui.html`), choose `POST /api/v1/rendezvous` and submit:

```json
{
  "patientId": 1,
  "creneauId": 123,
  "requestId": "4e0e4fc3-29b9-4823-952f-94cb1af78260"
}
```

Replace `patientId` and `creneauId` with the actual values from your running stack, and use a fresh UUID for each new booking. Click **Execute**.

Or use PowerShell:

```powershell
$patientId = 1       # replace with actual patient ID
$slotId = 123        # replace with an ID returned as available
$requestId = [guid]::NewGuid().ToString()
$body = @{ patientId = $patientId; creneauId = $slotId; requestId = $requestId } | ConvertTo-Json
$created = Invoke-RestMethod http://localhost:8083/api/v1/rendezvous `
  -Method Post -ContentType "application/json" -Body $body
$created | ConvertTo-Json -Depth 5
```

Expected: HTTP `201 Created` and a response containing the rendezvous `id`, patient and slot IDs, practitioner ID, status, and request ID.

### What this one POST demonstrates

1. Rendezvous checks its own database for the `requestId` (idempotency).
2. [`PatientClient`](rendezvous-service/src/main/java/com/medirdv/rdvcore/clients/PatientClient.java) calls patient REST: `GET http://patient-service:8081/api/v1/patients/{id}` and reads the insurance number.
3. [`AssuranceSoapClient`](rendezvous-service/src/main/java/com/medirdv/rdvcore/clients/AssuranceSoapClient.java), using `WebServiceTemplate` and JAXB, sends `verifierDroitsRequest` to `http://assurance-legacy:8080/ws`.
4. If the patient is covered, [`PraticienGrpcClient`](rendezvous-service/src/main/java/com/medirdv/rdvcore/clients/PraticienGrpcClient.java) calls unary `ReserverCreneau` at `praticien-service:9090`, passing the same request UUID and slot ID.
5. Rendezvous stores the appointment in its own `rdv_db` and records the practitioner ID returned by gRPC.

The rendezvous service has its own database; it stores references to patient and slot IDs rather than sharing their tables.

### Verify persistence and idempotency

In PowerShell, inspect the created record and repeat the exact same request body:

```powershell
Invoke-RestMethod "http://localhost:8083/api/v1/rendezvous/$($created.id)"
Invoke-RestMethod http://localhost:8083/api/v1/rendezvous `
  -Method Post -ContentType "application/json" -Body $body
```

The repeated POST should return HTTP `200 OK` with the existing rendezvous. It must use the same patient ID, slot ID, and request ID. Reusing that request ID for different data is rejected.

### Verify the slot was reserved

Use the praticien REST endpoint for the practitioner and slot returned in the booking:

```powershell
Invoke-RestMethod "http://localhost:8082/api/v1/praticiens/$($created.praticienId)/creneaux?statut=EN_ATTENTE"
```

The slot should no longer be free. In the domain, the reservation service places it in `EN_ATTENTE`; the practitioner can accept or refuse it using the relevant praticien Swagger endpoints.

---

## 6. Negative cases to demonstrate validation and error mapping

Use a new UUID for each request unless explicitly testing a replay.

| Scenario | How to trigger | Expected result |
|---|---|---|
| Covered patient | Patient with `ASSURE-001` + free slot | `201 Created`; reservation is persisted |
| No insurance coverage | Patient with `ASSURE-006` + free slot | `422`; no rendezvous is created and slot should not be reserved |
| Unknown patient | Use an ID that does not exist | `404`; patient REST 404 is translated by rendezvous |
| Unknown slot | Use an unused positive slot ID | `404`; gRPC `NOT_FOUND` is translated by rendezvous |
| Slot already reserved | Book one slot, then attempt another booking with a different UUID and that same slot | `409`; gRPC `FAILED_PRECONDITION` is translated by rendezvous |
| Idempotent replay | Repeat the exact same booking body | `200 OK`; existing rendezvous returned |
| Unknown insurance number | Direct SOAP request with a made-up number | SOAP business fault |

To inspect logs while demonstrating which downstream service was called:

```powershell
docker compose logs -f rendezvous-service praticien-service assurance-legacy patient-service
```

Press `Ctrl+C` to stop following logs; this does not stop the containers.

---

## 7. Close a rendezvous (optional SOAP declaration demo)

First accept the reservation in praticien Swagger using `PUT /api/v1/praticiens/{praticienId}/creneaux/{creneauId}/accepter`. A rendezvous booking initially leaves the slot in `EN_ATTENTE`; this acceptance changes the slot to `RESERVE` and the rendezvous to `PLANIFIE`. Then close the planned rendezvous. Rendezvous retrieves consultation metadata from praticien REST, re-fetches the patient over REST, then calls the second SOAP operation `declarerConsultation` on assurance.

In Swagger, call `PATCH /api/v1/rendezvous/{id}/statut` with:

```json
{
  "statut": "CLOTURE"
}
```

Expected: the rendezvous changes to `CLOTURE`; when the SOAP declaration succeeds, `pendingDeclaration` becomes false. The SOAP response contains an acknowledgement. This demonstrates the `declarerConsultationRequest` XSD element and its `date` and `praticien` fields.

---

## Quick oral summary

- **REST** uses HTTP methods and JSON resources; Swagger/OpenAPI describes the HTTP endpoints.
- **SOAP** uses XML envelopes. The XSD is the source contract; Spring-WS exposes a WSDL generated from it, JAXB converts XML to Java objects, and `@PayloadRoot` routes each operation to the endpoint method.
- **gRPC** uses HTTP/2 and Protocol Buffers. The `.proto` is the contract; Maven generates Java stubs; the praticien server implements the generated base class, and rendezvous calls it through a generated blocking stub.
- **Rendezvous is the orchestrator:** its REST API receives the booking, then it calls patient REST, assurance SOAP, and praticien gRPC, and stores the resulting appointment in its own database.
