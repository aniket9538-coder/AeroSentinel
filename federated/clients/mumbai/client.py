"""
AeroSentinel - Mumbai Coastal Node Client
File: federated/clients/mumbai/client.py

Coordinates local training rounds and packages parameter updates for the Mumbai coastal node.
"""

from typing import Optional, Any
from federated.clients.base_client import BaseFederatedClient
from federated.clients.mumbai.trainer import MumbaiTrainer
from federated.clients.mumbai.local_data import MUMBAI_NODE_ID, MUMBAI_CITY_ID


class MumbaiClient(BaseFederatedClient):
    """
    Mumbai coastal node federated client.
    """

    def __init__(self, random_state: int = 101, network_client: Optional[Any] = None):
        trainer = MumbaiTrainer(random_state=random_state)
        super().__init__(
            node_id=MUMBAI_NODE_ID,
            city_id=MUMBAI_CITY_ID,
            trainer=trainer,
            network_client=network_client,
        )
