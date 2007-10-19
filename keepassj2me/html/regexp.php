<?php
$secretCode = $_GET['code']; // '12341234';

if (preg_match('/^\d{8}$/', $secretCode) == 1) {
    echo 'Match <br/>';
} else {
    //echo 'No Match <br/>';
    header('HTTP/1.0 415 Unsupported Type');
}

?>
