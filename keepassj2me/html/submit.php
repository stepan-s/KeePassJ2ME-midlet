<?php
session_start();
?>

<?php
if (isset($_FILES['kdbfile'])) {
    $file = $_FILES['kdbfile'];
} else {
    echo '<strong>No file was uploaded.</strong><br/>';
    exit();
}

if (isset($_POST['usercode']) && isset($_POST['passcode'])) {
    $usercode = $_POST['usercode'];
    $passcode = $_POST['passcode'];
 } else {
    echo '<strong>No file was uploaded.</strong><br/>';
    exit();
}

$tmpname = $file['tmp_name'];

// move database file
$rv = move_uploaded_file($tmpname, "/var/www/kdb/Database-$usercode.kdb");
if (!$rv) {
    die ('Failed to upload the file');
}

createPassCodeFile($usercode, $passcode);

// create pass code file

if ($rv) {
    echo 'Your KDB file was uploaded successfully.  ';
    echo 'It will be removed from the server as soon as you download it from KeePass for J2ME.  ';
    echo 'If you do not download it, it will be removed after one hour.';
 } else {
    die ('Failed to upload the file');
 }

// generate n digit decimal number
function generateRandom($digit) {
  $result = "";
  if(($fhandle = fopen('/dev/random','rb')) != FALSE)
    {
      // set_magic_quotes_runtime(0);

      for($i=0; $i<$digit; $i++) {
	$val = ord(fgetc($fhandle)) % 10;
	$char = sprintf("%d", $val);
	$result .= "$char";
      }
    }

  return $result;
}

function createPassCodeFile($user, $pass)
{
    $filename = "/var/www/kdb/PassCode-$user";
    $fp = fopen($filename, 'w') or die("Can't create file");
    fwrite($fp, $pass) or die("Can't write file");
    fclose($fp);
}

?>



