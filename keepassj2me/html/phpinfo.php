<?php
session_start();
?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<meta http-equiv="content-type" content="text/html; charset=utf-8" />
	<title>Insert PHP Code</title>
</head>
<body>
<form action="submit.php" method="post" enctype="multipart/form-data">
     Username: <input type="text" name="username"/><br/>
     Password: <input type="password" name="password"/><br/>
     KDB file: <input type="file" name='kdbfile' /><br/>
     <input type="submit" name="submit" value="Submit" />     
</form>

<?php

// session
echo 'Session started<br/>';
echo 'Session ID: ' . session_id() . '<br/>';
$_SESSION['variable'] = 'This is my session variable!';
if (session_is_registered('variable')) {
    echo 'Session variable: ' . $_SESSION['variable'] . '<br/>';
 } else {
    echo 'No session variable<br/>';
 }


	function logInfo($message) {
		printf ("%s: File %s, Line %d<br/>", $message, __FILE__, __LINE__);
	}

	function sayHello($user) {
		echo "Hello Hello, $user<br/>";
	}

	define('GREETING', 'Hello, PHP!');	
	echo "<h1>" . GREETING . "</h1>";
	// this is a comment
	$i = 777;
	
	echo "The number is $i<br/>";

	$str = 'single quoted string';

	echo "$str<br />";

	$player1 = 8554;
	$player2 = 5666;

	if ($player1 > $player2) {
		echo 'Player 1 is the winner<br/>';
	} else {
		echo 'Congrats, Please 2<br/>';
	}

	// for loop
/*
	for ($i=0; $i<10; $i++) {
		echo "$i little Indian <br/>";
	}
*/

	// exit ("Let's stop here");

	sayHello('Rintaro');

	// array
	$array = array(3.14, 'Nao', 'Maki');

	print_r($array);

	print "<br/>";

	logInfo('Testing Log');

	printf ("Timestamp is %d<br/>", time());

// array
$days = array('Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun');
echo "First day of the week is: $days[0] <br/>";

// associative array
$book = array('author'=>'Toby', 'subject'=>'PHP5');
printf ("The book's author is %s <br/>", $book['author']);

// list
list($monday, $tuesday) = $days;

printf ("Tuesday == %s <br/>", $tuesday);

// file
/*
$handle = fopen('/tmp/file.txt', 'a+');
$data = fread($handle, 1024);

$writeData = "User:Nao\tIP:127.0.0.1\r";
fwrite($handle, $writeData);

fclose($handle);
echo "The file read was: <br/>$data<br/>";
*/

// cookie
setcookie('cookieName', 'cookieContent', time() + 3600);
echo 'Cookie has been set<br/>';

if (isset($_COOKIE['cookieName'])) {
    echo 'The value of the cookie is ' . $_COOKIE['cookieName'] . '<br/>';
 }

?>
</body>
</html>

