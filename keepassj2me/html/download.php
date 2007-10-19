<?php
$usercode = $_POST['usercode'];
$passcode = $_POST['passcode'];

// make sure $usercode is 4 digit number
if (preg_match('/^\d{4}$/', $usercode) != 1) {
    echo 'Invalid user code<br/>';
    header('HTTP/1.0 415 Unsupported Type');
    die('');
}

// make sure $passcode is 4 digit number
if (preg_match('/^\d{4}$/', $passcode) != 1) {
    echo 'Invalid pass code<br/>';
    header('HTTP/1.0 415 Unsupported Type');
    die('');
}

if (checkPassCode($usercode, $passcode) == FALSE) {
    echo 'Wrong pass code<br/>';
    header('HTTP/1.0 415 Unsupported Type');
    die('');
 }

// check if file exists
$filename='/var/www/kdb/Database-' . $usercode . '.kdb';
if (file_exists($filename) == FALSE) {
    header('HTTP/1.0 404 Not Found');
}

// send the file to client
header('Content-Type: application/octet-stream');
header('Content-Disposition: attachment; filename="'.$filename.'"');
@readfile($filename);

// remove the file
unlink ($filename);

function checkPassCode($user, $pass)
{
    $filename="/var/www/kdb/PassCode-$user";
    $fp = fopen($filename, 'r') or die("Can't open file");
    $passInFile = fread($fp, filesize($filename)) or die("Can't read file");
    fclose($fp);

    if ($pass == $passInFile)
	return TRUE;
    else
	return FALSE;
}
?>
