{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
              "@build.number": "${buildNumber}",
              "path": {
                "$nmatch": "org/springframework/boot/spring-boot-docs/*"
              }
            }
          ]
        }
      },
      "target": "nexus/"
    }
  ]
}
