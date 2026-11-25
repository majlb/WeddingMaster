import { useEffect } from "react";
import { Link } from "react-router-dom";
import "./privacy-policy.css";

export default function PrivacyPolicy() {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = "Privacy Policy | wedMasterAI";
    return () => { document.title = previousTitle; };
  }, []);

  return <div className="privacy-page" lang="en" dir="ltr">
    <header className="privacy-header">
      <Link to="/" className="privacy-brand">
        <img src="/favicon.png" alt="" />
        <span>.wedMasterAI</span>
      </Link>
      <Link to="/" className="privacy-home">Back to home</Link>
    </header>
    <main className="privacy-content">
      <p className="privacy-eyebrow">YOUR PRIVACY MATTERS</p>
      <h1>Privacy Policy</h1>
      <p className="privacy-intro">This policy explains how wedMasterAI handles information when you use our website and Android app.</p>
      <p className="privacy-updated">Last updated: November 23, 2026</p>

      <section>
        <h2>Who operates wedMasterAI</h2>
        <p>wedMasterAI is operated by Mohamad Jarrah. For privacy questions or requests, email <a href="mailto:maj.lb@hotmail.com">maj.lb@hotmail.com</a>.</p>
      </section>
      <section>
        <h2>Information we collect</h2>
        <ul>
          <li>Account information, such as your name, email address, password, phone number and account role. Passwords are stored as hashes.</li>
          <li>Wedding and planning information you choose to provide, such as your wedding date, preferences, checklist, budgets, guest names and phone numbers, and invitation details.</li>
          <li>Supplier profile and service information, including business contact details, location, pricing, availability, photographs and other uploaded media.</li>
          <li>Booking requests, quotations, comments, messages, attachments, reviews, notifications and payment records.</li>
          <li>Technical information generated when you use the service, such as IP addresses and server logs, and information stored locally on your browser or device to support sign-in and preferences.</li>
        </ul>
        <p>If you provide someone else's information, such as a guest's contact details, please ensure you have an appropriate reason and permission where required to share it.</p>
      </section>
      <section>
        <h2>How we use information</h2>
        <p>We use information to create and secure accounts, provide planning tools and supplier listings, process and manage bookings and payments, enable messages and notifications, provide support, maintain the service, and meet applicable legal obligations.</p>
        <p>When you use the AI assistant, your recent messages, selected countries and relevant public supplier information are sent to the configured AI provider to generate a response. Avoid including sensitive personal information or payment details in assistant messages.</p>
      </section>
      <section>
        <h2>Sharing and service providers</h2>
        <p>Supplier profile information and reviews may be visible to other users or the public. Booking requests, quotations, messages and attachments are shared with the relevant booking participants. Invitation information is shared with the recipients you choose.</p>
        <p>We use service providers to operate wedMasterAI. Depending on the features and configuration in use, these may include Render for hosting; Stripe for payment processing; OpenRouter and its selected AI model providers for the assistant; and Twilio and WhatsApp for messages and invitations. Stripe handles payment details through its own services. External resources, including Google Fonts, Unsplash and OpenStreetMap, may receive technical information when your device requests them.</p>
        <p>These providers process information under their own terms and privacy policies. We may also disclose information when required by law or to protect the service and its users.</p>
      </section>
      <section>
        <h2>Storage, security and retention</h2>
        <p>We use measures intended to protect information, including access controls and password hashing. No method of storage or transmission is completely secure. Your sign-in token and some preferences are stored on your browser or device; signing out removes the current sign-in details, but other locally stored preferences may remain until you clear them.</p>
        <p>We retain information as needed to operate the service, manage bookings and payments, resolve disputes, and meet legal obligations. Retention can vary by data type, and service providers may retain information under their own policies. Information may be processed in countries other than the country where you live.</p>
      </section>
      <section>
        <h2>Your choices and deletion requests</h2>
        <p>You can update certain profile and planning information within the app. To request access, correction, deletion of your account and associated data, or to raise another privacy concern, contact <a href="mailto:maj.lb@hotmail.com">maj.lb@hotmail.com</a>. Please identify your account and describe your request; do not email your password or payment details.</p>
        <p>We may need to verify your identity before acting. Some information may need to be retained for legal, payment, security or dispute-resolution purposes; we will explain any applicable limits when responding.</p>
      </section>
      <section>
        <h2>Children and changes to this policy</h2>
        <p>wedMasterAI is not intended for children. If you believe a child has provided personal information, please contact us. We may update this policy as the service changes; the latest version and its update date will be available on this page.</p>
      </section>
    </main>
    <footer className="privacy-footer">.wedMasterAI <span>·</span> <Link to="/">Home</Link></footer>
  </div>;
}
