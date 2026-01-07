"""
Multi-Region Deployment System

Deploys application to multiple regions sequentially.
Problems:
- Takes forever (deploys to each region one at a time)
- No visibility into per-region progress
- If one region fails halfway, hard to know what succeeded
- No automatic retries for transient cloud API failures
"""

import time
import random
from dataclasses import dataclass
from typing import List


@dataclass
class DeploymentConfig:
    app_name: str
    version: str
    regions: List[str]
    instance_count: int


@dataclass
class RegionDeployment:
    region: str
    status: str
    instances_deployed: int
    load_balancer_url: str
    deployment_time: float


class CloudAPI:
    """Simulates calling cloud provider APIs"""

    def provision_infrastructure(self, region: str, instance_count: int):
        """Provision VMs, networks, etc - takes 3-5 seconds"""
        print(f"  🏗️  [{region}] Provisioning {instance_count} instances...")
        time.sleep(random.uniform(3, 5))

        if random.random() < 0.12:
            raise Exception(f"Infrastructure provisioning failed in {region}")

        print(f"  ✓ [{region}] Infrastructure ready")

    def deploy_application(self, region: str, app_name: str, version: str):
        """Deploy app code - takes 4-7 seconds"""
        print(f"  📦 [{region}] Deploying {app_name} v{version}...")
        time.sleep(random.uniform(4, 7))

        if random.random() < 0.10:
            raise Exception(f"Application deployment failed in {region}")

        print(f"  ✓ [{region}] Application deployed")

    def configure_load_balancer(self, region: str):
        """Set up load balancer - takes 2-4 seconds"""
        print(f"  ⚖️  [{region}] Configuring load balancer...")
        time.sleep(random.uniform(2, 4))

        if random.random() < 0.08:
            raise Exception(f"Load balancer configuration failed in {region}")

        lb_url = f"https://lb-{region}.example.com"
        print(f"  ✓ [{region}] Load balancer ready: {lb_url}")
        return lb_url

    def run_health_checks(self, region: str):
        """Verify deployment health - takes 2-3 seconds"""
        print(f"  🏥 [{region}] Running health checks...")
        time.sleep(random.uniform(2, 3))

        if random.random() < 0.10:
            raise Exception(f"Health checks failed in {region}")

        print(f"  ✓ [{region}] All health checks passed")

    def update_dns(self, region: str, lb_url: str):
        """Update DNS records - takes 1-2 seconds"""
        print(f"  🌐 [{region}] Updating DNS to {lb_url}...")
        time.sleep(random.uniform(1, 2))

        print(f"  ✓ [{region}] DNS updated")


class DeploymentSystem:
    """Main system that deploys to regions - SEQUENTIALLY (slow!)"""

    def __init__(self):
        self.cloud_api = CloudAPI()

    def deploy_to_region(self, region: str, config: DeploymentConfig) -> RegionDeployment:
        """
        Deploy to a single region
        This is a multi-step process (5 steps)
        """
        print(f"\n{'=' * 70}")
        print(f"DEPLOYING TO {region}")
        print(f"{'=' * 70}")

        start_time = time.time()

        try:
            # Step 1: Provision infrastructure
            self.cloud_api.provision_infrastructure(region, config.instance_count)

            # Step 2: Deploy application
            self.cloud_api.deploy_application(region, config.app_name, config.version)

            # Step 3: Configure load balancer
            lb_url = self.cloud_api.configure_load_balancer(region)

            # Step 4: Run health checks
            self.cloud_api.run_health_checks(region)

            # Step 5: Update DNS
            self.cloud_api.update_dns(region, lb_url)

            elapsed = time.time() - start_time

            print(f"\n✅ [{region}] Deployment complete! (took {elapsed:.1f}s)")

            return RegionDeployment(
                region=region,
                status="deployed",
                instances_deployed=config.instance_count,
                load_balancer_url=lb_url,
                deployment_time=elapsed
            )

        except Exception as e:
            elapsed = time.time() - start_time
            print(f"\n❌ [{region}] Deployment failed: {e} (after {elapsed:.1f}s)")

            return RegionDeployment(
                region=region,
                status="failed",
                instances_deployed=0,
                load_balancer_url="",
                deployment_time=elapsed
            )

    def deploy_all_regions(self, config: DeploymentConfig) -> List[RegionDeployment]:
        """
        Deploy to all regions
        PROBLEM: Does them ONE AT A TIME - takes forever!
        """
        print("\n" + "=" * 70)
        print(f"MULTI-REGION DEPLOYMENT: {config.app_name} v{config.version}")
        print(f"Regions: {', '.join(config.regions)}")
        print("=" * 70)

        start_time = time.time()
        results = []

        # ========================================
        # PROBLEM: Deploy to each region SEQUENTIALLY
        # These are independent - could run in PARALLEL!
        # ========================================
        for region in config.regions:
            result = self.deploy_to_region(region, config)
            results.append(result)

        # Summary
        elapsed = time.time() - start_time
        successful = [r for r in results if r.status == "deployed"]
        failed = [r for r in results if r.status == "failed"]

        print("\n" + "=" * 70)
        print(f"DEPLOYMENT SUMMARY")
        print("=" * 70)
        print(f"Total time: {elapsed:.1f} seconds (SLOW!)")
        print(f"Successful: {len(successful)}/{len(config.regions)}")
        if successful:
            for r in successful:
                print(f"  ✓ {r.region}: {r.load_balancer_url}")
        if failed:
            print(f"Failed: {len(failed)}")
            for r in failed:
                print(f"  ✗ {r.region}")
        print("=" * 70 + "\n")

        return results


def main():
    """Run a multi-region deployment"""

    config = DeploymentConfig(
        app_name="MyWebApp",
        version="2.1.0",
        regions=["us-west-2", "us-east-1", "eu-west-1"],
        instance_count=3
    )

    system = DeploymentSystem()
    results = system.deploy_all_regions(config)

    print(f"\nFinal results: {len([r for r in results if r.status == 'deployed'])} successful")


if __name__ == "__main__":
    main()