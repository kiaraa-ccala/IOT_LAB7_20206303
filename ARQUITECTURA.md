# ARQUITECTURA BiciPUCP

## 1. Patron arquitectonico global

La solucion aplica una arquitectura de microservicios. El backend se divide en servicios pequenos y especializados: `eureka-server` descubre servicios, `pucp-validador-service` concentra las reglas de validacion academica e IoT, y `orquestador-service` coordina el flujo que consume Android. Este patron permite que cada componente tenga una responsabilidad clara, pueda ejecutarse en paralelo y sea visible desde Eureka.

## 2. Stateless en el microservicio validador

`pucp-validador-service` cumple la restriccion Stateless porque no guarda sesion, estado de usuario ni informacion temporal entre peticiones. Cada endpoint recibe todos los datos necesarios en la URL (`codigo` o `pin`), calcula la respuesta con reglas puras y devuelve `true` o `false`. Dos llamadas iguales producen la misma respuesta sin depender de memoria del servidor.

## 3. Flujo aplicado

Android no crea el usuario directamente en Firebase. Primero envia `{ "codigo": "...", "pin": "..." }` al orquestador por `POST /bici/solicitar-desbloqueo`. El orquestador valida alumno con `RestTemplate`, valida candado con `FeignClient`, y solo si ambas reglas aprueban devuelve un token IoT, 120 segundos de vigencia y `timestamp_aprobacion`. Con ese resultado la app registra al usuario en Firebase Auth y guarda el perfil en Firestore.
