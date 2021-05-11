## Objetivos:
  - Hacer funcionar Comerzzia en PostgreSQL

- Documentación de PostgreSQL
https://www.postgresql.org/docs/11/datatype-binary.html

## Compatibilidad con Blob de SQL Standard
- En PostgreSQL hay 2 sistemas para manejar Large Objects: TOAST o BLOB/CLOT


[TOAST](https://www.enterprisedb.com/postgres-tutorials/postgresql-toast-and-working-blobsclobs-explained)
  - Usa el tipo bytea
  - Los scripts actuales, al crear tablas usando `bytea` para Blob están usando este sistema.

BLOB/CLOT
    - Usa los tipos Blob/Clot
    - Todos los objetos se almacenan en una tabla `pg_largeobject`
    - Al usar las funciones SQL o la API libq, PostgreSQL guarda una referencia al Blob en un campo `OID`, que hace referencia a otra tabla.
    - Es en la tabla `pg_largeobject` donde se almacena el verdadero blob, junto con todos los blobs.
    - Los datos de `pg_largeobject` están almacenados en bytea troceados en varios registros.
    - Para manejar este sistema hay dos alternativas: 
   1.   Funciones SQL 
        - lo_creat()
        - lo_create()
        - lo_unlink()
        - lo_import()
        - lo_export()
   2. Large Object API, que usa libpq
  - Sin embargo dan problemas de integración con mybatis

## SOLUCIÓN ALTERNATIVA
TOAST + TypeHandler
- Permite mantener los mappers de comerzzia intactos
- Nota: Actualmente esta solución sólo funciona en el standard implementado con Mybatis-Spring
- Para hacerlo funcionar con las clases DAO antiguas pensamos que habría que configurar el fichero `mybatis-config.xml` correctamento y/o modificar las anotaciones de la clase `CustomBlobTypeHandler.java`. Más información en la [documentación de Mybatis](https://mybatis.org/mybatis-3/configuration.html)