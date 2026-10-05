{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
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
