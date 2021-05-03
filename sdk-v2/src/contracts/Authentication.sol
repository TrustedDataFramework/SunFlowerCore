// SPDX-License-Identifier: GPL-3.0

pragma solidity >=0.7.0 <0.9.0;

interface Authentication {
    // approve another node
    function approve(address dst) external;
    
    // try to join, wait for approve
    function join() external;
    
    // exit 
    function exit() external;
    
    // get approved addresses
    function approved() external view returns (address[] memory);

    // returns approves for this pending 
    function pending(address dst)  external view returns (address[] memory); 
}