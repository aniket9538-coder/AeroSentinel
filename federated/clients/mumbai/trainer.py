"""
AeroSentinel - Mumbai Coastal Node Trainer
File: federated/clients/mumbai/trainer.py

Trains local hotspot classification model on Mumbai municipal coastal data.
"""

from federated.clients.base_client import BaseCityTrainer
from federated.clients.mumbai.local_data import (
    MumbaiLocalDataLoader,
    MUMBAI_NODE_ID,
    MUMBAI_CITY_ID,
)


class MumbaiTrainer(BaseCityTrainer):
    """
    Mumbai local model trainer.
    """

    def __init__(self, random_state: int = 101):
        data_loader = MumbaiLocalDataLoader(random_seed=random_state)
        super().__init__(
            node_id=MUMBAI_NODE_ID,
            city_id=MUMBAI_CITY_ID,
            data_loader=data_loader,
            random_state=random_state,
        )
