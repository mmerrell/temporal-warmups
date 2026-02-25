# Temporalizing Strategy

## 1. Create the workflow

Call `public ReviewResponse review(ReviewRequest request)`

Follow execution steps in `ReviewOrchestrator.java` to finish the workflow

### What is required in a workflow?

- Only deterministic activities

1. Implements interface
2. configure how activities should behave - timeouts, retries
3. create activity stubs with non-existent interfaces. Using Workflow
4. call the workflow method

## 2. Create the Starter using a 6-step process
//1. set a task queue name in an interface for reuse
//2. create main
//3. connect to temporal server, same as Worker
//4. create workflowId
String workflowId = TASK_QUEUE + "-" + UUID.randomUUID().toString().substring(0, 8);
//5. create a workflow stub
//6. call the workflow

## 3. Create Worker using 5-step process

This is the more involved step. It will require creating the `ActivityImpl` and the business logic objects.

        // 1. Connect to Temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // 2. Create worker factory and worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(Shared.TASK_QUEUE);

        // 3. Register workflow implementation
        // Q: Why register the IMPL class, not the interface?
        // A: Temporal needs to instantiate the class to run workflows.
        worker.registerWorkflowImplementationTypes(BatchPRReviewWorkflowImpl.class);

        //4. Tell worker the activities that it can run
        //register the Activities and pass in business logic
        //first time we are creating ActivityImpls
        worker.registerActivitiesImplementations(
                new CodeQualityActivityImpl(codeQualityAgent),
                new TestQualityActivityImpl(testQualityAgent),
                new SecurityQualityActivityImpl(securityAgent)
        );

        // 5. Start the worker (blocks forever, listening for tasks)
        factory.start();

## 4. (Optional) Update POM.xml for simple CLI execution

```bash
# Terminal 2: Temporal Worker
mvn compile exec:java@worker

# Terminal 3: Temporal Client (Starter)
mvn compile exec:java@workflow
```