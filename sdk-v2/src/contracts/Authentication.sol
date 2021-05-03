// SPDX-License-Identifier: GPL-3.0

pragma solidity >=0.7.0 <0.9.0;

interface Authentication {
    // approve another miner
    function approve(address dst) external;
    
    // try to join as miner, wait for approves >= 2/3 approved
    function join() external;
    
    // exit 
    function exit() external;
    
    // get approved miners
    function approved() external view returns (address[] memory);

    // returns approved for this pending 
    function pending(address dst)  external view returns (address[] memory); 
}