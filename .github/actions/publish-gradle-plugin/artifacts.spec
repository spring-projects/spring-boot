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
                "$match": "org/springframework/boot/spring-boot-gradle-plugin/*"
              }
            }
          ]
        }
      },
      "target": "repository/"
    }
  ]
}
