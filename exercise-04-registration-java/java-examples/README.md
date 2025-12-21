## Key Java vs Python Differences

| Concept | Python | Java |
|---------|--------|------|
| **Activities** | `@activity.defn` decorator on function | Interface + Implementation class |
| **Workflows** | `@workflow.defn` class with `@workflow.run` | Interface + Implementation class |
| **Activity calls** | `await workflow.execute_activity(func, args=[...])` | `activities.methodName(...)` via stub |
| **Workflow execution** | `await client.execute_workflow(Workflow.run, args=[...])` | `workflow.methodName(...)` via stub |
| **Options** | Dict/kwargs | Builder pattern |
| **Worker setup** | Register classes/functions in lists | `registerWorkflowImplementationTypes()` + `registerActivitiesImplementations()` |
| **Async/Await** | `async def` + `await` everywhere | No async/await (blocking style) |
| **Type hints** | Optional, using Python types | Required, using Java types |
| **Package structure** | Flat files in directory | Package hierarchy (`package helloworld;`) |
| **Dependencies** | `pip install temporalio` | Maven/Gradle (`temporal-sdk` artifact) |